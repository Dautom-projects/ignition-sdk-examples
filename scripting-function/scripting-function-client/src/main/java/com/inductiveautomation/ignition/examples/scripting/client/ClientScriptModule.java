package com.inductiveautomation.ignition.examples.scripting.client;

import com.inductiveautomation.ignition.examples.scripting.AbstractScriptModule;
import com.inductiveautomation.ignition.examples.scripting.MathBlackBox;
import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;

public class ClientScriptModule extends AbstractScriptModule {

    private final MathBlackBox rpc;

    public ClientScriptModule() {
        rpc = ModuleRPCFactory.create(
            "com.inductiveautomation.ignition.examples.scripting-function",
            MathBlackBox.class
        );
    }

    @Override
    protected int multiplyImpl(int arg0, int arg1) {
        return rpc.multiply(arg0, arg1);
    }
    @Override
    protected void browseTagsImpl() throws Exception {
        rpc.browseTags();
    }
    @Override
    protected void createTagsImpl() throws Exception {
        rpc.createTags();
    }
    @Override
    protected void writeReadTagValueImpl() throws Exception {
        rpc.writeReadTagValue();
    }
    @Override
    protected void writeReadTagPropertyImpl() throws Exception {
        rpc.writeReadTagProperty();
    }
    @Override
    protected void writeReadUdtParameterImpl() throws Exception {
        rpc.writeReadUdtParameter();
    }
    @Override
    protected void copyMoveRenameTagImpl() throws Exception {
        rpc.copyMoveRenameTag();
    }

    @Override
    protected void deleteTagsImpl() throws Exception {
        rpc. deleteTags();
    }



}
