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
    public void createTags() throws Exception {
    createTagsImpl();
    }
    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void writeReadTagValue() throws Exception {
        writeReadTagValueImpl();
    }
    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void writeReadTagProperty() throws Exception {
        writeReadTagPropertyImpl();
    }
    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void writeReadUdtParameter() throws Exception {
        writeReadUdtParameterImpl();
    }
    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void copyMoveRenameTag() throws Exception {
        copyMoveRenameTagImpl();
    }
    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void deleteTags() throws Exception {
        deleteTagsImpl();
    }
    @Override
    @ScriptFunction(docBundlePrefix = "AbstractScriptModule")
    public void editTags() throws Exception {
        editTagsImpl();
    }

    protected abstract int multiplyImpl(int arg0, int arg1);
    protected abstract void browseTagsImpl() throws Exception;
    protected abstract void createTagsImpl() throws Exception;
    protected abstract void writeReadTagValueImpl() throws Exception;
    protected abstract void writeReadTagPropertyImpl() throws Exception;
    protected abstract void writeReadUdtParameterImpl() throws Exception;
    protected abstract void copyMoveRenameTagImpl() throws Exception;
    protected abstract void deleteTagsImpl() throws Exception;
    protected abstract void editTagsImpl() throws Exception;

}
