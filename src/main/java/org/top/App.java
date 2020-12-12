package org.top;

import lombok.extern.slf4j.Slf4j;
import org.top.core.ServerStateTransformerStarter;

import java.io.File;

/**
 * @author lubeilin
 * @date 2020/11/5
 */
@Slf4j
public class App {
    public static void main(String[] args) {
        new ServerStateTransformerStarter().start();
    }
}
