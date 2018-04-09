package wishful_framework.Config;


import wishful_framework.WishFulConfig;

public class D2DAgentConfig extends WishFulConfig {
    public D2DAgentConfig() {

        this.yamlConfigString = "## WiSHFUL Agent config file\n" +
                "\n" +
                "agent_config:\n" +
                "  name: 'D2D client'\n" +
                "  info: 'config for client running D2D content exchange'\n" +
                "\n" +
                "modules:\n" +
                "  wifi_control:\n" +
                "    module : ewine_d2d_wifi_control_module\n" +
                "    class_name : D2DWiFiControlModule\n" +
                "\n" +
                "  content_control:\n" +
                "    module : ewine_d2d_content_control_module\n" +
                "    class_name : D2DContentControlModule\n" +
                "\n" +
                "  test:\n" +
                "    module : wishful_test_module\n" +
                "    class_name : TestModule\n";

    }
}
