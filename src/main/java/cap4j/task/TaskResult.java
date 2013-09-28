/*
 * Copyright (C) 2013 Andrey Chaschev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cap4j.task;

import cap4j.scm.CommandLineResult;
import cap4j.session.Result;

/**
 * @author Andrey Chaschev chaschev@gmail.com
 */
public class TaskResult {
    public Result result;
    public CommandLineResult cliResult;

    public TaskResult(CommandLineResult cliResult) {
        this.cliResult = cliResult;
        this.result = cliResult.result;
    }

    public TaskResult(Result result, String text) {
        this.result = result;
        cliResult = new CommandLineResult(text, result);
    }


    public TaskResult(Result result) {
        this.result = result;
    }

    public TaskResult(Result result, CommandLineResult cliResult) {
        this.result = result;
        this.cliResult = cliResult;
    }

    public static final TaskResult OK = new TaskResult(Result.OK);

    public static TaskResult and(TaskResult... results){
        for (TaskResult result : results) {
            if(!result.ok()){
                return result;
            }
        }

        return results[results.length - 1];
    }

    public boolean ok() {
        return result.ok();
    }

    public boolean nok() {
        return result.nok();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TaskResult{");
        sb.append("result=").append(result);
        sb.append(", cliResult=").append(cliResult);
        sb.append('}');
        return sb.toString();
    }
}
