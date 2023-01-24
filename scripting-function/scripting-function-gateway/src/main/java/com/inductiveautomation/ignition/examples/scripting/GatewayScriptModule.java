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
    protected void writeReadTagValueImpl() throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed

        TagPath memoryTag0 = TagPathParser.parse("MemoryTag0");
        QualifiedValue newQv = new BasicQualifiedValue(43);

        // Write out a new value. Note that we are using SecurityContext.emptyContext(), as we are not working with
        // user roles or security zones in this example.
        List<QualityCode> results = provider.writeAsync(Arrays.asList(memoryTag0), Arrays.asList(newQv), SecurityContext.emptyContext()).get(30, TimeUnit.SECONDS);

        QualityCode qc = results.get(0);
        if(qc.isNotGood()) {
            throw new Exception(String.format("Write tag value operation returned bad result '%s'", qc.toString()));
        }

        // Read the updated value
        List<QualifiedValue> values = provider.readAsync(Arrays.asList(memoryTag0), SecurityContext.emptyContext())
                .get(30, TimeUnit.SECONDS);
        QualifiedValue qv = values.get(0);
        if(qv.getQuality().isNotGood()) {
            throw new Exception(String.format("MemoryTag0 cannot be read, quality=" + qv.getQuality().toString()));
        } else {
            String qvValue = qv.getValue() != null ? qv.getValue().toString(): "null";
            logger.info("MemoryTag0 value='%s'", qvValue);
        }
    }

    protected void writeReadTagPropertyImpl() throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed

        TagPath memoryTag1Doc = TagPathParser.parse("LevelOne_FolderA/MemoryTag1." + WellKnownTagProps.Documentation.getName());
        QualifiedValue newQv = new BasicQualifiedValue("Updated documentation for MemoryTag1");

        // Write out the tag property. Note that we are using SecurityContext.emptyContext(), as we are not working with
        // user roles or security zones in this example.
        List<QualityCode> results = provider.writeAsync(Arrays.asList(memoryTag1Doc), Arrays.asList(newQv), SecurityContext.emptyContext()).get(30, TimeUnit.SECONDS);

        QualityCode qc = results.get(0);
        if (qc.isNotGood()) {
            throw new Exception(String.format("Write tag property operation returned bad result '%s'", qc.toString()));
        }

        // Read the tag property.
        List<QualifiedValue> values = provider.readAsync(Arrays.asList(memoryTag1Doc), SecurityContext.emptyContext())
                .get(30, TimeUnit.SECONDS);
        QualifiedValue qv = values.get(0);
        if (qv.getQuality().isNotGood()) {
            throw new Exception(String.format("MemoryTag1.documentation cannot be read, quality=" + qv.getQuality().toString()));
        } else {
            String qvValue = qv.getValue() != null ? qv.getValue().toString(): "null";
            logger.info("MemoryTag1 documentation='%s'", qvValue);
        }
    }

    protected void writeReadUdtParameterImpl() throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed

        TagPath udtParam = TagPathParser.parse("BasicUDT_OverrideInstance0/parameters/MyParam");
        QualifiedValue newQv = new BasicQualifiedValue("Updated param for Basic UDT override instance");

        // Write out the udt parameter. Note that we are using SecurityContext.emptyContext(), as we are not working with
        // user roles or security zones in this example.
        List<QualityCode> results = provider.writeAsync(Arrays.asList(udtParam), Arrays.asList(newQv), SecurityContext.emptyContext()).get(30, TimeUnit.SECONDS);

        QualityCode qc = results.get(0);
        if (qc.isNotGood()) {
            throw new Exception(String.format("Write udt parameter operation returned bad result '%s'", qc.toString()));
        }

        List<QualifiedValue> values = provider.readAsync(Arrays.asList(udtParam), SecurityContext.emptyContext())
                .get(30, TimeUnit.SECONDS);
        QualifiedValue qv = values.get(0);
        if (qv.getQuality().isNotGood()) {
            throw new Exception(String.format("BasicUDT_OverrideInstance0/MyParam cannot be read, quality=" + qv.getQuality().toString()));
        } else {
            String qvValue = qv.getValue() != null ? qv.getValue().toString() : "null";
            logger.info("BasicUDT_OverrideInstance0 MyParam ='%s'", qvValue);
        }
    }


}
