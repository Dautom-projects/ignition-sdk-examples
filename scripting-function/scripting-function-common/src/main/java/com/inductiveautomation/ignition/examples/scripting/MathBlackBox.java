package com.inductiveautomation.ignition.examples.scripting;

import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

public interface MathBlackBox {

    public int multiply(int arg0, int arg1);
    public void browseTags() throws Exception;
    public void createTags() throws Exception;
    public void writeReadTagValue() throws Exception;
    public void writeReadTagProperty() throws Exception;
    public void writeReadUdtParameter() throws Exception;
    public void copyMoveRenameTag() throws Exception;
    public void deleteTags() throws Exception;
    public void editTags() throws Exception;
    public  void importTags() throws Exception;
    public void triggerTagGroupExecution();
    public void assembly_paths();
}
