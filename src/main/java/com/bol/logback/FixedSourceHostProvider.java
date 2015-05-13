package com.bol.logback;

public class FixedSourceHostProvider implements SourceHostProvider {

    private String sourceHost;

    public FixedSourceHostProvider( String sourceHost ) {
        this.sourceHost = sourceHost;
    }

    public String getSourceHost() {
        return sourceHost;
    }
}
