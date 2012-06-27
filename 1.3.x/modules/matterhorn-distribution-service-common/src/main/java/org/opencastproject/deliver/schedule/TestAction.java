/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.deliver.schedule;

import org.opencastproject.deliver.schedule.Action;
// import org.opencastproject.deliver.schedule.RetryException;
// import org.opencastproject.deliver.schedule.FailedException;
import org.opencastproject.deliver.store.InvalidKeyException;

public class TestAction extends Action {

    private int execute_count = 0;
    private int execute_limit = 3;
    private boolean fails = false;
    private boolean retries = false;
    private long deadline_seconds = Action.DEFAULT_TASK_SECONDS;

    public TestAction() {
        super(null);
    }

    public TestAction(String name) {
        super(name);
    }

    public int getExecuteCount() {
        return execute_count;
    }

    public void setExecuteCount(int execute_count) {
        this.execute_count = execute_count;
    }

    public int getExecuteLimit() {
        return execute_limit;
    }

    public void setExecuteLimit(int execute_limit) {
        this.execute_limit = execute_limit;
    }

    public boolean getFails() {
        return fails;
    }

    public void setFails(boolean fails) {
        this.fails = fails;
    }

    public boolean getRetries() {
        return retries;
    }

    public void setRetries(boolean retries) {
        this.retries = retries;
    }

    public void setDeadlineSeconds(long deadline_seconds) {
        this.deadline_seconds = deadline_seconds;
    }

    protected long deadlineSeconds() {
        return deadline_seconds;
    }

    protected void execute() throws InvalidKeyException {
        execute_count += 1;
        System.out.println("Execute #" + execute_count
                + ", limit=" + execute_limit);
        if (execute_count < execute_limit)
            resumeAfter(1);
        else if (retries)
            ; // throw new RetryException("Retry task.", 1);
        else if (fails)
            ; // throw new FailedException("oops!");
        else
            succeed();
    }

}
