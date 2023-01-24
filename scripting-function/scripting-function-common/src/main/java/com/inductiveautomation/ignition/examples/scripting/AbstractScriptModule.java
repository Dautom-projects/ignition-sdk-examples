package com.inductiveautomation.ignition.examples.scripting;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.script.hints.ScriptArg;
import com.inductiveautomation.ignition.common.script.hints.ScriptFunction;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

import java.io.IOException;

public abstract class AbstractScriptModule implements MathBlackBox {

    static {
        BundleUtil.get().addBundle(
            AbstractScriptModule.class.getSimpleName(),
            AbstractScriptModule.class.getClassLoader(),
            AbstractScriptModule.class.getName().replace('.', '/')
        );
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public int multiply(
        @ScriptArg("arg0") int arg0,
        @ScriptArg("arg1") int arg1) {

        return multiplyImpl(arg0, arg1);
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void browseTags() throws Exception {
        browseTagsImpl();
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void triggerTagGroupExecution() {
        triggerTagGroupExecutionImpl();
    }


    protected abstract int multiplyImpl(int arg0, int arg1);
    protected abstract void browseTagsImpl() throws Exception;
    protected abstract void triggerTagGroupExecutionImpl();



}
