package cn;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class jiangzeyin {
    public static void main(String[] args) {

        Properties properties = System.getProperties();
        for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {
            Map.Entry entry = (Map.Entry) objectObjectEntry;
            System.out.print(entry.getKey() + "=");
            System.out.println(entry.getValue());
        }
    }
}
