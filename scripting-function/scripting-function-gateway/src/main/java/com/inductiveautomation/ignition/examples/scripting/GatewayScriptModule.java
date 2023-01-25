package com.inductiveautomation.ignition.examples.scripting;

import com.inductiveautomation.ignition.common.Path;
import com.inductiveautomation.ignition.common.alarming.config.*;
import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.config.BasicProperty;
import com.inductiveautomation.ignition.common.config.Property;
import com.inductiveautomation.ignition.common.model.values.BasicQualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.sqltags.BasicTagPermissions;
import com.inductiveautomation.ignition.common.sqltags.history.BasicTagHistoryQueryParams;
import com.inductiveautomation.ignition.common.sqltags.model.TagPermissionsModel;
import com.inductiveautomation.ignition.common.sqltags.model.scripts.BasicTagEventScripts;
import com.inductiveautomation.ignition.common.sqltags.model.scripts.TagEventScripts;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;
import com.inductiveautomation.ignition.common.sqltags.model.types.SQLQueryType;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
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
import com.inductiveautomation.ignition.gateway.datasource.BasicStreamingDataset;
import com.inductiveautomation.ignition.gateway.history.HistoryManager;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.sqltags.history.GatewayTagHistoryManager;
import com.inductiveautomation.ignition.gateway.sqltags.history.TagHistoryProvider;
import com.inductiveautomation.ignition.gateway.tags.evaluation.groups.TagGroupManager;
import com.inductiveautomation.ignition.gateway.tags.evaluation.providers.ProviderContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.inductiveautomation.ignition.common.script.builtin.DateUtilities.now;

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
    protected void createTagsImpl() throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed
        logger.info("provider: " + provider);

        List<TagConfiguration> newTagConfigs = new ArrayList<>();

        // Create a folder
        TagPath levelOneFolderA = TagPathParser.parse("LevelOne_FolderA");
        TagConfiguration tagConfig = BasicTagConfiguration.createNew(levelOneFolderA);
        tagConfig.setType(TagObjectType.Folder);
        newTagConfigs.add(tagConfig);
        logger.info("tagConfig: " + tagConfig);

        // Create a simple memory tag
        TagPath memoryTag0 = TagPathParser.parse("MemoryTag0");
        tagConfig = BasicTagConfiguration.createNew(memoryTag0);
        tagConfig.setType(TagObjectType.AtomicTag);
        tagConfig.set(WellKnownTagProps.ValueSource, WellKnownTagProps.MEMORY_TAG_TYPE);
        tagConfig.set(WellKnownTagProps.DataType, DataType.Int4);
        tagConfig.set(WellKnownTagProps.Value, new BasicQualifiedValue(42));
        newTagConfigs.add(tagConfig);
        logger.info("tagConfig: " + tagConfig);
        logger.info("tag: " + memoryTag0);

        // Create a more complex memory tag, put it under LevelOne_FolderA
        TagPath memoryTag1 = TagPathParser.parse("LevelOne_FolderA/MemoryTag1");
        tagConfig = BasicTagConfiguration.createNew(memoryTag1);
        tagConfig.setType(TagObjectType.AtomicTag);
        tagConfig.set(WellKnownTagProps.ValueSource, WellKnownTagProps.MEMORY_TAG_TYPE);
        tagConfig.set(WellKnownTagProps.DataType, DataType.Int4);
        tagConfig.set(WellKnownTagProps.Value, new BasicQualifiedValue(1));
        tagConfig.set(WellKnownTagProps.Documentation, "This is MemoryTag1");
        tagConfig.set(WellKnownTagProps.Tooltip, "MemoryTag1 tooltip");
        logger.info("tagConfig: " + tagConfig);
        logger.info("tag: " + memoryTag1);

        // Alarm configuration
        AlarmConfiguration alarmConfig = new BasicAlarmConfiguration();

        AlarmDefinition alarmOneDefinition = new BasicAlarmDefinition();
        alarmOneDefinition.setName("HighValue");
        alarmOneDefinition.set(AlarmModeProperties.Mode, AlarmMode.AboveValue);
        alarmOneDefinition.set(AlarmModeProperties.SetpointA, 5.0d);
        alarmConfig.add(alarmOneDefinition);

        AlarmDefinition alarmTwoDefinition = new BasicAlarmDefinition();
        alarmTwoDefinition.setName("LowValue");
        alarmTwoDefinition.set(AlarmModeProperties.Mode, AlarmMode.BelowValue);
        alarmTwoDefinition.set(AlarmModeProperties.SetpointA, 1.0d);
        alarmConfig.add(alarmTwoDefinition);
        tagConfig.set(WellKnownTagProps.Alarms, alarmConfig);


        // Tag permissions
        Map<TagPermissionsModel.ZoneRole, Boolean> permissionsMap = new HashMap<>();
        TagPermissionsModel.ZoneRole zoneRole = new TagPermissionsModel.ZoneRole();
        zoneRole.setRole("Administrator");
        zoneRole.setZone("default");
        permissionsMap.put(zoneRole, true);
        BasicTagPermissions permissions = new BasicTagPermissions(permissionsMap);
        tagConfig.set(WellKnownTagProps.PermissionModel, permissions);

        // Tag event scripts
        TagEventScripts scripts = new BasicTagEventScripts();
        scripts.set("onValueChange", "print 'Hello World'");
        tagConfig.set(WellKnownTagProps.EventScripts, scripts);

        // Some custom tag properties
        Property<String> strProp = new BasicProperty<>("customStr", String.class);
        tagConfig.set(strProp, "Hello world");

        Property<Integer> intProp = new BasicProperty<>("customInt", Integer.class);
        tagConfig.set(intProp, 42);

        Property<Double> doubleProp = new BasicProperty<>("customDbl", Double.class);
        tagConfig.set(doubleProp, 3.1415d);
        newTagConfigs.add(tagConfig);


        // Create an expression tag
        TagPath expressionTag0 = TagPathParser.parse("ExpressionTag0");
        tagConfig = BasicTagConfiguration.createNew(expressionTag0);
        tagConfig.setType(TagObjectType.AtomicTag);
        tagConfig.set(WellKnownTagProps.DataType, DataType.DateTime);
        tagConfig.set(WellKnownTagProps.FormatString, "yyyy-MM-dd h:mm:ss aa");
        tagConfig.set(WellKnownTagProps.ValueSource, ExpressionTypeProperties.TAG_TYPE);
        tagConfig.set(ExpressionTypeProperties.Expression, "now(1000)");
        newTagConfigs.add(tagConfig); // Command added  to salve tag configuration in the tag list
        logger.info("tagConfig: " + tagConfig);
        logger.info("tag: " + expressionTag0);

        // Create a query tag
        TagPath queryTag0 = TagPathParser.parse("QueryTag0");
        tagConfig = BasicTagConfiguration.createNew(queryTag0);
        tagConfig.setType(TagObjectType.AtomicTag);
        tagConfig.set(WellKnownTagProps.DataType, DataType.DateTime);
        tagConfig.set(WellKnownTagProps.FormatString, "yyyy-MM-dd h:mm:ss aa");
        tagConfig.set(WellKnownTagProps.ValueSource, DBTagTypeProperties.TAG_TYPE);
        tagConfig.set(WellKnownTagProps.ExecutionMode, TagExecutionMode.TagGroupRate);  // Tag group will control the tag's execution
        tagConfig.set(DBTagTypeProperties.QueryType, SQLQueryType.Select);
        tagConfig.set(DBTagTypeProperties.QueryDatasource, "my_datasource");
        tagConfig.set(DBTagTypeProperties.Query, "select now()");
        newTagConfigs.add(tagConfig); // Command added  to salve tag configuration in the tag list
        logger.info("tagConfig: " + tagConfig);
        logger.info("tag: " + queryTag0);

        // Create an OPC tag
        TagPath opcTag0 = TagPathParser.parse("OpcTag0");
        tagConfig = BasicTagConfiguration.createNew(opcTag0);
        tagConfig.setType(TagObjectType.AtomicTag);
        tagConfig.set(WellKnownTagProps.DataType, DataType.Float4);
        tagConfig.set(WellKnownTagProps.ValueSource, OpcTagTypeProperties.TAG_TYPE);
        tagConfig.set(OpcTagTypeProperties.OPCServer, "Ignition OPC UA Server");
        tagConfig.set(OpcTagTypeProperties.OPCItemPath, "ns=1;s=[GenericSimulator]_Meta:Writeable/WriteableFloat1");
        newTagConfigs.add(tagConfig); // Command added  to salve tag configuration in the tag list
        logger.info("tagConfig: " + tagConfig);
        logger.info("tag: " + opcTag0);

        // Create simple UDT definition and add a memory tag
        TagPath tinyUdtDef = TagPathParser.parse("_types_/TinyUdtDef");
        tagConfig = BasicTagConfiguration.createNew(tinyUdtDef);
        tagConfig.setType(TagObjectType.UdtType);
        newTagConfigs.add(tagConfig);
        logger.info("tagConfig: " + tagConfig);
        logger.info("tag: " + tinyUdtDef);

        TagPath udtDefMember = TagPathParser.parse("_types_/TinyUdtDef/MemoryTagMember");
        tagConfig = BasicTagConfiguration.createNew(udtDefMember);
        tagConfig.setType(TagObjectType.AtomicTag);
        tagConfig.set(WellKnownTagProps.DataType, DataType.Int4);
        tagConfig.set(WellKnownTagProps.Value, new BasicQualifiedValue(1));
        newTagConfigs.add(tagConfig);
        logger.info("tagConfig: " + tagConfig);
        logger.info("tag: " + udtDefMember);

        // Create simple UDT instance
        TagPath udtInstance = TagPathParser.parse("TinyUdt_Instance");
        tagConfig = BasicTagConfiguration.createNew(udtInstance);
        tagConfig.setType(TagObjectType.UdtInstance);
        tagConfig.set(WellKnownTagProps.TypeId, "TinyUdtDef");
        newTagConfigs.add(tagConfig);
        logger.info("tagConfig: " + tagConfig);
        logger.info("tag: " + udtInstance);

        CompletableFuture<List<QualityCode>> future =
                provider.saveTagConfigsAsync(newTagConfigs, CollisionPolicy.Abort);
        logger.info("future: " + future);

        List<QualityCode> results = future.get(30, TimeUnit.SECONDS);
        logger.info("results: " + results);

        for (int i = 0; i < results.size(); i++) {
            QualityCode result = results.get(i);
            if (result.isNotGood()) {
                throw new Exception(String.format("Add tag operation returned bad result for tag '%s'", newTagConfigs.get(i).getName()));
            }
        }
        logger.info("result: " + results);

        newTagConfigs.clear();

        // Create another simple UDT instance and override the value of MemoryTagMember. Note how the udt definition
        // must first be created in the previous call. We supply the full path of a udt member and only provide the
        // properties that should be overridden.
        TagPath udtOverrideInstance = TagPathParser.parse("TinyUdt_OverrideInstance");
        tagConfig = BasicTagConfiguration.createNew(udtOverrideInstance);
        tagConfig.setType(TagObjectType.UdtInstance);
        tagConfig.set(WellKnownTagProps.TypeId, "TinyUdtDef");
        newTagConfigs.add(tagConfig);

        TagPath udtOverrideMember = TagPathParser.parse("TinyUdt_OverrideInstance/MemoryTagMember");
        tagConfig = BasicTagConfiguration.createNew(udtOverrideMember);
        tagConfig.set(WellKnownTagProps.Value, new BasicQualifiedValue(2));
        newTagConfigs.add(tagConfig);

        future =
                provider.saveTagConfigsAsync(newTagConfigs, CollisionPolicy.Abort);

        future.get(30, TimeUnit.SECONDS);

        for (int i = 0; i < results.size(); i++) {
            QualityCode result = results.get(i);
            if (result.isNotGood()) {
                throw new Exception(String.format("Add tag operation returned bad result for tag '%s'", newTagConfigs.get(i).getName()));
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
            logger.info("MemoryTag0 value = " + qvValue);
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
            logger.info("MemoryTag1 documentation = " + qvValue);
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
            logger.info("BasicUDT_OverrideInstance0 MyParam = " + qvValue);
        }
    }
    @Override
    protected void copyMoveRenameTagImpl() throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();

        // We are going thru the GatewayTagManager rather than a specific tag provider, so we must add the provider name
        // to the front of the path.
        TagPath memoryTag1 = TagPathParser.parse("[default]LevelOne_FolderA/MemoryTag1");
        logger.info("Tag path to copy: " + memoryTag1);
        TagPath destination = TagPathParser.parse("[default]LevelOne_FolderA");
        logger.info("Path to the folder where the copied tag will be pasted (if it is the same folder, rename the tag): " + destination);

        // Make a copy of LevelOne_FolderA/MemoryTag1
        List<QualityCode> results = tagManager.moveTagsAsync(Arrays.asList(memoryTag1), destination, true, CollisionPolicy.Rename).get(30, TimeUnit.SECONDS);

        QualityCode qc = results.get(0);
        if (qc.isNotGood()) {
            throw new Exception(String.format("Copy operation returned bad result '%s'", qc.toString()));
        }

        // Now move the newly copied tag to the root
        TagPath memoryTag2 = TagPathParser.parse("[default]LevelOne_FolderA/MemoryTag2");
        logger.info("Path of the tag to move: " + memoryTag2);
        destination = TagPathParser.parse("[default]");
        logger.info("Destination where the tag will be moved: " + destination);


        results = tagManager.moveTagsAsync(Arrays.asList(memoryTag2), destination, false, CollisionPolicy.Abort).get(30, TimeUnit.SECONDS);
        qc = results.get(0);
        if (qc.isNotGood()) {
            throw new Exception(String.format("Move operation returned bad result '%s'", qc.toString()));
        }

        // Finally, rename MemoryTag2 to RootMemoryTag1
        memoryTag2 = TagPathParser.parse("[default]MemoryTag2");
        logger.info("Path of the tag to rename: " + memoryTag2);
        String newName = "RootMemoryTag1"; // variable created
        logger.info("New tag name: " + newName);
        //results = tagManager.renameTag(memoryTag2, "RootMemoryTag1", CollisionPolicy.Abort).get(30, TimeUnit.SECONDS);  ---replaced by line 110
        results = tagManager.renameTag(memoryTag2, newName, CollisionPolicy.Abort).get(30, TimeUnit.SECONDS);

        qc = results.get(0);
        if (qc.isNotGood()) {
            throw new Exception(String.format("Rename operation returned bad result '%s'", qc.toString()));
        }

    }
    @Override
    protected void deleteTagsImpl() throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed


        List<TagPath> toDelete = new ArrayList<>();
        TagPath levelOne_FolderA = TagPathParser.parse("LevelOne_FolderA");
        logger.info("Name of the folder to be deleted: " + levelOne_FolderA);
        toDelete.add(levelOne_FolderA);

        TagPath tinyUdtOverrideInstance = TagPathParser.parse("TinyUdt_OverrideInstance");
        logger.info("Name of the tag to be deleted: " + tinyUdtOverrideInstance);
        toDelete.add(tinyUdtOverrideInstance);


        List<QualityCode> results = provider.removeTagConfigsAsync(toDelete)
                .get(30, TimeUnit.SECONDS);

        for (int i = 0; i < results.size(); i++) {
            QualityCode result = results.get(i);
            if (result.isNotGood()) {
                TagPath tagPath = toDelete.get(i);
                throw new Exception(String.format("Delete tag operation for tag '%s' returned bad result '%s'",
                        tagPath.toStringFull(), result.toString()));
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
    @Override
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
    @Override
    protected void triggerTagGroupExecutionImpl() {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed
        logger.info("provider: " + provider);
        // Requests an extra execution of the Default tag group
        if (provider instanceof ProviderContext) {
            TagGroupManager groupManager = ((ProviderContext) provider).getTagGroupManager();
            String nameTagProvider = "Default"; //Name tag provider to execution
            groupManager.requestExecution(nameTagProvider);
            //groupManager.requestExecution("Default");
            logger.info("Tag Provider execution: " + nameTagProvider);
        }
    }

    @Override
    protected void queryTagHistoryImpl(Date startTime,Date endTime) throws Exception {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagHistoryManager tagHistoryManager = context.getTagHistoryManager();
        HistoryManager historyManager = context.getHistoryManager();
        TagHistoryProvider historyProvider = tagHistoryManager.getTagHistoryProvider("scales_simulator");  // Change tag provider name here as needed
        Path attempt1 = TagPathParser.parse("[scales_simulator]molino a/scale h1/pv_totalweightnoclearable");
        logger.info("path: " + attempt1);

        List<? extends Path> newPaths = new ArrayList<Path>();
        logger.info("path list: " + newPaths);
        newPaths = Arrays.asList(attempt1);
        //histprov:plant_db:/drv:gw-dau-rd-yield-calculator:scales_simulator:/tag:molino a/scale b1/pv_actualpower

        BasicStreamingDataset dataStream = new BasicStreamingDataset();

        BasicTagHistoryQueryParams newquery = new BasicTagHistoryQueryParams();
        newquery.setPaths(newPaths);
        logger.info("newquery path list: " + newquery.getPaths());
        newquery.setEndDate(endTime);
        logger.info("newquery enddate: " + newquery.getEndDate());
        newquery.setStartDate(startTime);
        logger.info("newquery stardate: " + newquery.getStartDate());
        /*Change null to set different value
        newquery.setAliases();
        logger.info("newquery aliases: " + newquery.getAliases());
        newquery.setColumnAggregationModes(null);
        logger.info("newquery agregationmodes: " + newquery.getColumnAggregationModes());
         */
        logger.info("newquery: " + newquery);

        tagHistoryManager.queryHistory(newquery,dataStream);
        logger.info("datastream: " + dataStream);
        //comando para correr el script en el scriptconsole
        //system.example.queryTagHistory(system.date.getDate(2023, 0, 25),system.date.getDate(2023, 0, 23))
    }
}