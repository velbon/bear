package cap4j.examples;

import cap4j.Stage;
import cap4j.session.*;
import cap4j.task.Task;

import static cap4j.session.GenericUnixRemoteEnvironment.newUnixRemote;

/**
 * User: chaschev
 * Date: 7/21/13
 */
public class Ex1Basic {
    public static void main(String[] args) {
        new GenericUnixRemoteEnvironment()
            .setSshAddress(
                new GenericUnixRemoteEnvironment.SshAddress("chaschev", "aaaaaa", "192.168.25.66")
            );

        final Stage pacDev = new Stage("pac-dev")
            .add(newUnixRemote("chaschev", "aaaaaa", "192.168.25.66"))
            ;

        final Task<Task.TaskResult> testTask = new Task<Task.TaskResult>() {
            {

            }

            @Override
            protected TaskResult run() {
                system.copy("src", "dest");
                system.runForEnvironment("linux", new SystemEnvironments.EnvRunnable() {
                    @Override
                    public Result run(SystemEnvironment system) {
                        return system.run("echo blahblahblah").result;
                    }
                });

                return new TaskResult(Result.OK);
            }
        };

        pacDev.runTask(testTask);
    }
}
