package com.hdu.bean;

public class SimToken {

    public Byte tokenHash;
    public int line;
    public int measureID;

    public SimToken(Byte tokenHash, int line, int measureID) {
        this.tokenHash = tokenHash;
        this.line = line;
        this.measureID = measureID;
    }

    @Override
    public String toString() {
        return "SimToken{" +
                "tokenHash=" + tokenHash +
                ", line=" + line +
                ", measureID=" + measureID +
                '}';
    }
}
