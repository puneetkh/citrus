/*
 * Copyright 2006-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.dsl.endpoint.runner;

import com.consol.citrus.dsl.builder.*;
import com.consol.citrus.dsl.runner.ExecutableTestRunnerComponent;
import com.consol.citrus.message.MessageType;
import org.springframework.stereotype.Component;

@Component("BarTestRunner")
public class BarTestRunner extends ExecutableTestRunnerComponent {

    @Override
    public void execute() {
        receive(new BuilderSupport<ReceiveMessageBuilder>() {
            @Override
            public void configure(ReceiveMessageBuilder builder) {
                builder.endpoint("inboundChannelEndpoint")
                        .messageType(MessageType.PLAINTEXT)
                        .payload("<TestRunner name=\"BarTestRunner\"></TestRunner>");
            }
        });

        send(new BuilderSupport<SendMessageBuilder>() {
            @Override
            public void configure(SendMessageBuilder builder) {
                builder.endpoint("inboundChannelEndpoint")
                        .payload("<TestRunner name=\"BarTestRunner\">OK</TestRunner>");
            }
        });

        echo("Bar TestRunner OK!");
    }
}
