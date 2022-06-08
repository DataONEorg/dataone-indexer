/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright 2022
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 */

package org.dataone.cn.index;

/**
 * Worker class to process index tasks and submit results to store.
 */
public class IndexWorker {

    /**
     * Commandline main for the IndexWorker to be started.
     * @param args
     */
    public static void main(String[] args) {

        System.out.println("Starting index worker...");
        IndexWorker worker = new IndexWorker();
        
        // TODO: read RabbitMQ and SOLR config info from environment

        // TODO: register this worker with RabbitMQ

        worker.handleIndexTask("123");
        System.out.println("Done.");
    }

    /**
     * Callback for processing a specific indexing task
     * @param message the message describing the task to be processed
     */
    public void handleIndexTask(String message) {
        System.out.println("Handling task: " + message);
        // TODO: Handle the index task
    }
}
