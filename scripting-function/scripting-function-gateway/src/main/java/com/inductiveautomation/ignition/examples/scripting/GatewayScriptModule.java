package com.inductiveautomation.ignition.examples.scripting;

import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

public class GatewayScriptModule extends AbstractScriptModule {

    @Override
    protected int multiplyImpl(int arg0, int arg1) {
        return arg0 * arg1;
    }

    @Override
    protected String browseTagsImpl() {
        GatewayContext context = GatewayHook.getGatewayContext();
        GatewayTagManager tagManager = context.getTagManager();
        TagProvider provider = tagManager.getTagProvider("default");  // Change tag provider name here as needed

        return provider.toString();
    }

}
