package com.inductiveautomation.ignition.examples.scripting;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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


}
