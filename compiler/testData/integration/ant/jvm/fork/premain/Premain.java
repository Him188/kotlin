import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

public class Premain {
    public static void premain(String agentArgs, Instrumentation inst) {
        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = RuntimemxBean.getInputArguments();
        arguments.stream().filter(param ->
            param.startsWith("-Xmx") || param.startsWith("-Xms")
        ).sorted().forEach(System.out::println);
    }
}