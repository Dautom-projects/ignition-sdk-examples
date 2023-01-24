package com.inductiveautomation.ignition.examples.scripting;

import com.inductiveautomation.ignition.common.alarming.config.*;
import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.config.BasicProperty;
import com.inductiveautomation.ignition.common.config.Property;
import com.inductiveautomation.ignition.common.model.values.BasicQualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.BasicTagPermissions;
import com.inductiveautomation.ignition.common.sqltags.model.TagPermissionsModel;
import com.inductiveautomation.ignition.common.sqltags.model.scripts.BasicTagEventScripts;
import com.inductiveautomation.ignition.common.sqltags.model.scripts.TagEventScripts;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;
import com.inductiveautomation.ignition.common.sqltags.model.types.SQLQueryType;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.*;
import com.inductiveautomation.ignition.common.tags.config.properties.WellKnownTagProps;
import com.inductiveautomation.ignition.common.tags.config.types.DBTagTypeProperties;
import com.inductiveautomation.ignition.common.tags.config.types.ExpressionTypeProperties;
import com.inductiveautomation.ignition.common.tags.config.types.OpcTagTypeProperties;
import com.inductiveautomation.ignition.common.tags.config.types.TagObjectType;
import com.inductiveautomation.ignition.common.tags.model.SecurityContext;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GatewayScriptModule extends AbstractScriptModule {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected int multiplyImpl(int arg0, int arg1) {
        return arg0 * arg1;
    }

    @Override
    protected void browseTagsImpl() throws Exception{
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed

        TagPath root = TagPathParser.parse("");
        browseNode(provider, root);
        logger.info("provider: " + provider);
    }

    private void browseNode(TagProvider provider, TagPath parentPath) throws Exception {
        Results<NodeDescription> results = provider.browseAsync(parentPath, BrowseFilter.NONE).get(30, TimeUnit.SECONDS);

        if(results.getResultQuality().isNotGood()) {
            throw new Exception("Bad quality results: "+ results.getResultQuality().toString());
        }

        Collection<NodeDescription> nodes = results.getResults();
        StringBuilder structure = new StringBuilder();
        for(int i = 0; i<parentPath.getPathLength(); i++) {
            structure.append("\t");
        }

        String formatted = structure.toString() + "[%s] objectType=%s, dataType=%s, subTypeId=%s, currentValue=%s, displayFormat=%s, attributes=%s, hasChildren=%s";
        for(NodeDescription node: nodes) {
            String currentValue = node.getCurrentValue().getValue() != null ? node.getCurrentValue().getValue().toString(): "null";
            String descr = String.format(formatted, node.getName(),
                    node.getObjectType(),
                    node.getDataType(),
                    node.getSubTypeId(),
                    currentValue,
                    node.getDisplayFormat(),
                    node.getAttributes().toString(),
                    node.hasChildren());
                    logger.info(descr);

            // Browse child nodes, but not Document nodes such as UDT parameters
            if(node.hasChildren() && DataType.Document != node.getDataType()) {
                TagPath childPath = parentPath.getChildPath(node.getName());
                browseNode(provider, childPath);
            }
        }
    }

    @Override
    protected void editTagsImpl() throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed
        logger.info("provider: " + provider);

        TagPath memoryTag0 = TagPathParser.parse("MemoryTag0");

        // MemoryTag0 will be the first item in the returned list. We get back a TagConfigurationModel
        // that we can modify and send back. Note that if MemoryTag0 doesn't actually exist, the TagConfigurationModel's
        // TagObjectType will be TagObjectType.Unknown.
        List<TagConfigurationModel> configs = provider.getTagConfigsAsync(Arrays.asList(memoryTag0), false, true).get(30, TimeUnit.SECONDS);
        TagConfigurationModel tagConfig = configs.get(0);
        if (TagObjectType.Unknown == tagConfig.getType()) {
            throw new Exception("MemoryTag0 edit configuration not found");
        }

        // Add some documentation to the tag
        tagConfig.set(WellKnownTagProps.Documentation, "Some documentation for MemoryTag0");
        logger.info("Documentation: " + tagConfig.get(WellKnownTagProps.Documentation));

        // And now save the tag. Use the MergeOverwrite collision policy to merge in the documentation property but
        // keep other tag properties intact.
        List<QualityCode> results = provider.saveTagConfigsAsync(Arrays.asList(tagConfig), CollisionPolicy.MergeOverwrite).get(30, TimeUnit.SECONDS);

        for (int i = 0; i < results.size(); i++) {
            QualityCode result = results.get(i);
            if (result.isNotGood()) {
                throw new Exception(String.format("Edit tag operation returned bad result '%s'", result.toString()));
            }
        }
    }

}
