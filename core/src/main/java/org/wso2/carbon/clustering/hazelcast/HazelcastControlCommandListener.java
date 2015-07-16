/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.clustering.hazelcast;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.clustering.ControlCommand;
import org.wso2.carbon.clustering.exception.MessageFailedException;


/**
 * The message listener using with hazelcast based distributed topics
 * for sending/receiving cluster control messages from/to cluster
 */
public class HazelcastControlCommandListener implements MessageListener<ControlCommand> {
    private static Logger logger = LoggerFactory.getLogger(HazelcastControlCommandListener.class);

    @Override
    public void onMessage(Message<ControlCommand> controlCommand) {
        try {
            logger.info("Received ControlCommand: " + controlCommand.getMessageObject());
            controlCommand.getMessageObject().execute();
        } catch (MessageFailedException e) {
            logger.error("Cannot process ControlCommand", e);
        }
    }
}
