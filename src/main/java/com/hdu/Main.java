package com.hdu;

import com.hdu.processor.SRCFProcessor;

public class Main {

    public static void main(String[] args) {
        SRCFProcessor srcfProcessor = new SRCFProcessor(args);
        srcfProcessor.run();
    }
}
