package wishful_framework;


import java.util.ArrayList;

public class WishFulDevice {
    public String name;

    WishFulDevice(String name) {
        this.name = name;
    }

    static ArrayList<WishFulDevice> parse(Object obj) {
        if (!(obj instanceof ArrayList)) {
            return null;
        }
        ArrayList<String> list = (ArrayList<String>)obj;
        ArrayList<WishFulDevice> devList = new ArrayList<WishFulDevice>();
        for (String s : list) {
            devList.add(new WishFulDevice(s));
        }
        return devList;
    }

}
