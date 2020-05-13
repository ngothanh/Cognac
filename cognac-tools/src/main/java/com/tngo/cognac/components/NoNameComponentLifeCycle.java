package com.tngo.cognac.components;

public abstract class NoNameComponentLifeCycle extends AbstractComponentLifeCycle {

    private static final String NO_NAME = "No-Name";

    @Override
    protected String generateName() {
        return NO_NAME;
    }
}
