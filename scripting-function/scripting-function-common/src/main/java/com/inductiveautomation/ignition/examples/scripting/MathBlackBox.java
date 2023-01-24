package com.inductiveautomation.ignition.examples.scripting;

import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

public interface MathBlackBox {

    public int multiply(int arg0, int arg1);

    public String browseTags() throws Exception;

}
