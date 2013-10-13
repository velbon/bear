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

package bear.session;

import bear.console.AbstractConsole;
import bear.console.CompositeConsole;
import bear.console.ProgressMonitor;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Andrey Chaschev chaschev@gmail.com
 */


//todo don't extends
//todo change to index
public class SystemEnvironments extends CompositeConsole{


    public SystemEnvironments(List<? extends AbstractConsole> consoles, ProgressMonitor progressMonitor, ExecutorService executorService) {
        super(consoles, progressMonitor, executorService);
    }
}