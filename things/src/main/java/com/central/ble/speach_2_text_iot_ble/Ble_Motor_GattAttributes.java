package com.central.ble.speach_2_text_iot_ble;

import java.util.HashMap;

/**
 * Created by sinjo.mattappallil on 10/18/2017.
 */

public class Ble_Motor_GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String MOTOR_CONTROL_SERVICE = "0000A000-0000-1000-8000-00805F9B34FB";
    public static String MOTOR_CHARACTERISTIC_CONTROL = "0000A001-0000-1000-8000-00805F9B34FB";

    static {
        // Sample Services.
        attributes.put("0000A000-0000-1000-8000-00805F9B34FB", "Motor control Service");
        attributes.put("0000A001-0000-1000-8000-00805F9B34FB", "Motor control charecteristic");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
