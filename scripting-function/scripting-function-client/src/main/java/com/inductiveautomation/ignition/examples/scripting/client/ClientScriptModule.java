package com.inductiveautomation.ignition.examples.scripting.client;

import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.examples.scripting.AbstractScriptModule;
import com.inductiveautomation.ignition.examples.scripting.MathBlackBox;
import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

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
    protected String browseTagsImpl() {
        return rpc.browseTags();
    }

}
