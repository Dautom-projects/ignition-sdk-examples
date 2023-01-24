package com.inductiveautomation.ignition.examples.scripting;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    protected void importTagsImpl() throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed

        // Note that it is normally better to import tag json from a File object. We are using Strings here to keep
        // the example simple.
        String basicUdtDef = getUdtDefImport(); // name of the methods that return the json of the tags to be imported
        String udtInstances = getUdtInstancesImport(); //name of the methods that return the json of the tags to be imported

        // Import the definition first
        TagPath importPath = TagPathParser.parse("_types_");
        // Using the Ignore collision policy here to make sure we don't accidentally overwrite existing tags.
        List<QualityCode> results = provider.importTagsAsync(importPath, basicUdtDef, "json", CollisionPolicy.Ignore).get(30, TimeUnit.SECONDS);

        for (int i = 0; i < results.size(); i++) {
            QualityCode result = results.get(i);
            if (result.isNotGood()) {
                throw new Exception(String.format("Add tag operation returned bad result '%s'", result.toString()));
            }
            logger.info("Imported tag status: " + result);

        }

        // Then import the instances. Note that if the UDT definitions and instances are in the same tag json import file, then we
        // don't need to perform two separate imports.
        importPath = TagPathParser.parse("");
        results = provider.importTagsAsync(importPath, udtInstances, "json", CollisionPolicy.Ignore)
                .get(30, TimeUnit.SECONDS);

        for (int i = 0; i < results.size(); i++) {
            QualityCode result = results.get(i);
            if (result.isNotGood()) {
                throw new Exception(String.format("Add tag operation returned bad result '%s'", result.toString()));
            }
            logger.info("Imported tag status: " + result);

        }
    }

    private String getUdtDefImport() { //method with the tags json
        return "{\n"
                + "  \"dataType\": \"Int4\",\n"
                + "  \"name\": \"BasicUDTDef\",\n"
                + "  \"value\": 0,\n"
                + "  \"parameters\": {\n"
                + "    \"MyParam\": \"paramval\",\n"
                + "    \"MyIntegerParam\": -1.0\n"
                + "  },\n"
                + "  \"tagType\": \"UdtType\",\n"
                + "  \"tags\": [\n"
                + "    {\n"
                + "      \"eventScripts\": [\n"
                + "        {\n"
                + "          \"eventid\": \"valueChanged\",\n"
                + "          \"script\": \"\\tprint \\\"Value changed!\\\"\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"valueSource\": \"memory\",\n"
                + "      \"dataType\": \"Int4\",\n"
                + "      \"name\": \"MemberEventScriptTag\",\n"
                + "      \"value\": 10,\n"
                + "      \"tagType\": \"AtomicTag\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"UdtFolderLevelOne\",\n"
                + "      \"tagType\": \"Folder\",\n"
                + "      \"tags\": [\n"
                + "        {\n"
                + "          \"valueSource\": \"memory\",\n"
                + "          \"dataType\": \"Float4\",\n"
                + "          \"alarms\": [\n"
                + "            {\n"
                + "              \"mode\": \"AboveValue\",\n"
                + "              \"setpointA\": 100.0,\n"
                + "              \"name\": \"HighValue\",\n"
                + "              \"priority\": \"High\"\n"
                + "            }\n"
                + "          ],\n"
                + "          \"name\": \"BasicTypeAlarmTag\",\n"
                + "          \"value\": 0.5,\n"
                + "          \"tagType\": \"AtomicTag\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"valueSource\": \"expr\",\n"
                + "          \"expression\": \"{MyParam}\",\n"
                + "          \"dataType\": \"String\",\n"
                + "          \"expressionType\": \"Expression\",\n"
                + "          \"name\": \"BasicTypeExpressionTag\",\n"
                + "          \"value\": \"\",\n"
                + "          \"tagType\": \"AtomicTag\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"name\": \"UdtFolderLevelTwo\",\n"
                + "          \"tagType\": \"Folder\",\n"
                + "          \"tags\": [\n"
                + "            {\n"
                + "              \"documentation\": {\n"
                + "                \"bindType\": \"parameter\",\n"
                + "                \"binding\": \"MyIntegerParam set to {MyIntegerParam}\"\n"
                + "              },\n"
                + "              \"tooltip\": {\n"
                + "                \"bindType\": \"parameter\",\n"
                + "                \"binding\": \"UDT instance {MyParam}\"\n"
                + "              },\n"
                + "              \"valueSource\": \"memory\",\n"
                + "              \"dataType\": \"Int4\",\n"
                + "              \"name\": \"LevelTwoMemoryTag\",\n"
                + "              \"value\": 2,\n"
                + "              \"tagType\": \"AtomicTag\"\n"
                + "            }\n"
                + "          ]\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}";
    }

    private String getUdtInstancesImport() { //method with the tags json
        return "{\n"
                + "  \"tags\": [\n"
                + "    {\n"
                + "      \"dataType\": \"Int4\",\n"
                + "      \"name\": \"BasicUDT_Instance0\",\n"
                + "      \"typeId\": \"BasicUDTDef\",\n"
                + "      \"value\": 0,\n"
                + "      \"parameters\": {\n"
                + "        \"MyParam\": \"Param for Basic UDT Instance\",\n"
                + "        \"MyIntegerParam\": 0.0\n"
                + "      },\n"
                + "      \"tagType\": \"UdtInstance\",\n"
                + "      \"tags\": [\n"
                + "        {\n"
                + "          \"name\": \"MemberEventScriptTag\",\n"
                + "          \"tagType\": \"AtomicTag\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"name\": \"UdtFolderLevelOne\",\n"
                + "          \"tagType\": \"Folder\",\n"
                + "          \"tags\": [\n"
                + "            {\n"
                + "              \"name\": \"BasicTypeExpressionTag\",\n"
                + "              \"tagType\": \"AtomicTag\"\n"
                + "            },\n"
                + "            {\n"
                + "              \"name\": \"UdtFolderLevelTwo\",\n"
                + "              \"tagType\": \"Folder\",\n"
                + "              \"tags\": [\n"
                + "                {\n"
                + "                  \"name\": \"LevelTwoMemoryTag\",\n"
                + "                  \"tagType\": \"AtomicTag\"\n"
                + "                }\n"
                + "              ]\n"
                + "            },\n"
                + "            {\n"
                + "              \"name\": \"BasicTypeAlarmTag\",\n"
                + "              \"tagType\": \"AtomicTag\"\n"
                + "            }\n"
                + "          ]\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"dataType\": \"Int4\",\n"
                + "      \"name\": \"BasicUDT_OverrideInstance0\",\n"
                + "      \"typeId\": \"BasicUDTDef\",\n"
                + "      \"value\": 0,\n"
                + "      \"parameters\": {\n"
                + "        \"MyParam\": \"Param for Basic UDT override instance\",\n"
                + "        \"MyIntegerParam\": 1.0\n"
                + "      },\n"
                + "      \"tagType\": \"UdtInstance\",\n"
                + "      \"tags\": [\n"
                + "        {\n"
                + "          \"eventScripts\": [\n"
                + "            {\n"
                + "              \"eventid\": \"valueChanged\",\n"
                + "              \"script\": \"\\tprint \\\"Override: Value changed!\\\"\"\n"
                + "            }\n"
                + "          ],\n"
                + "          \"name\": \"MemberEventScriptTag\",\n"
                + "          \"tagType\": \"AtomicTag\"\n"
                + "        },\n"
                + "        {\n"
                + "          \"name\": \"UdtFolderLevelOne\",\n"
                + "          \"tagType\": \"Folder\",\n"
                + "          \"tags\": [\n"
                + "            {\n"
                + "              \"name\": \"UdtFolderLevelTwo\",\n"
                + "              \"tagType\": \"Folder\",\n"
                + "              \"tags\": [\n"
                + "                {\n"
                + "                  \"value\": \"4\",\n"
                + "                  \"name\": \"LevelTwoMemoryTag\",\n"
                + "                  \"tagType\": \"AtomicTag\"\n"
                + "                }\n"
                + "              ]\n"
                + "            },\n"
                + "            {\n"
                + "              \"enabled\": false,\n"
                + "              \"name\": \"BasicTypeExpressionTag\",\n"
                + "              \"tagType\": \"AtomicTag\"\n"
                + "            },\n"
                + "            {\n"
                + "              \"alarms\": [\n"
                + "                {\n"
                + "                  \"mode\": \"AboveValue\",\n"
                + "                  \"setpointA\": 200.0,\n"
                + "                  \"name\": \"HighValue\",\n"
                + "                  \"priority\": \"High\"\n"
                + "                },\n"
                + "                {\n"
                + "                  \"mode\": \"BelowValue\",\n"
                + "                  \"setpointA\": 10.0,\n"
                + "                  \"name\": \"LowValue\"\n"
                + "                }\n"
                + "              ],\n"
                + "              \"name\": \"BasicTypeAlarmTag\",\n"
                + "              \"tagType\": \"AtomicTag\"\n"
                + "            }\n"
                + "          ]\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}";
    }
}
