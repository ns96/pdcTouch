/*
 * This class simulates the connection to the actual hardware
 */
package pdctouch;

import java.util.LinkedHashMap;

/**
 *
 * @author nathan
 */
public class PDCTouchDevice {
    public int motorCount;
    public int maxPoles = 22;
        
    private boolean calibrated = true;
    
    public int motorPoles = 0;
    public float a = 10;
    public float b = 10000;
    public float c = 0;
    public float d = 0;
    
    // arrays for soting the ramp mode 
    // format is [min rpm, min time, min pwm, max rpm, max time, max pwm]
    private int[] rampMode1;
    private int[] rampMode2;
    private int[] rampMode3;
    private int[] rampMode4;
    
    // store the motor information is hashmap
    private LinkedHashMap<String, Float[]> motorInfo = new LinkedHashMap<>();
    
    // stores the current motor information
    private Float[] currentMotorInfo;
    
    // The index of the startup device
    public int startUpIndex = 0;
    
    public PDCTouchDevice() {
        // store information about particular motor
        motorInfo.put("Park 480 @ 9V", new Float[]{7f, 7.5f, 7500f, 0f, 0f});
        motorInfo.put("Park 480 @ 12V", new Float[]{7f, 10.3f, 10300f, 0f, 0f});
        motorInfo.put("Park 450 @ 9V", new Float[]{7f, 7.0f, 7000f, 0f, 0f});
        motorInfo.put("Park 450 @ 12V", new Float[]{7f, 9.8f, 9800f, 0f, 0f});
        motorInfo.put("D2836/11 @ 9V", new Float[]{7f, 6.6f, 6600f, 0f, 0f});
        motorInfo.put("Other @ 9/12V", new Float[]{3f, 8.8f, 8800f, 0f, 0f});
        
        motorCount = motorInfo.size();
        
        rampMode1 = new int[]{500, 10, getPWM(500), 3100, 35, getPWM(3100)};
        rampMode2 = new int[]{600, 10, getPWM(600), 2700, 30, getPWM(2700)};
        rampMode3 = new int[]{700, 15, getPWM(700), 2500, 40, getPWM(2500)};
        rampMode4 = new int[]{800, 10, getPWM(800), 2000, 25, getPWM(2000)};
    }
    
    public String getMotorName(int index) {
        return (String)motorInfo.keySet().toArray()[index];
    }
    
    public void setCurrentMotor(String name) {
        currentMotorInfo = motorInfo.get(name);
        motorPoles = Math.round(currentMotorInfo[0]);
        a = currentMotorInfo[1];
        b = currentMotorInfo[2];
        c = currentMotorInfo[3];
        d = currentMotorInfo[4];
    }
    
    /**
     * Get the particular ramp mode data
     * @param mode
     * @return 
     */
    public int[] getRampModeData(int mode) {
        switch (mode) {
            case 1:
                return rampMode1;
            case 2:
                return rampMode2;
            case 3:
                return rampMode3;
            default:
                return rampMode4;
        }
    }
    
    /**
     * Set the Ramp Mode
     * 
     * @param mode
     * @param rampMode 
     */
    public void setRampModeData(int mode, int[] rampMode) {
        switch (mode) {
            case 1:
                rampMode1 = rampMode;
            case 2:
                rampMode2 = rampMode;
            case 3:
                rampMode3 = rampMode;
            default:
                rampMode4 = rampMode;
        }
    }
    
    /**
     * Return the speed giving a pwmValue
     * @param pwmValue
     * @return 
     */
    public int getSpeed(int pwmValue) {
        float y = a * pwmValue - b;
        return (int)y;
    }
    
    /**
     * Function to get the pwmValue giving the speed
     * 
     * @param rpmSpeed 
     * @return  
     */
    public int getPWM(int rpmSpeed) {
        float x = (rpmSpeed + b)/a;
        return (int)x;
    }
    
    public boolean isCalibrated() {
        return calibrated;
    }

    void saveMotorInfo() {
        currentMotorInfo[0] = new Float(motorPoles);
        currentMotorInfo[1] = a;
        currentMotorInfo[2] = b;
        currentMotorInfo[3] = c;
        currentMotorInfo[4] = d;
        
        System.out.println("Saved motor information ...");
    }
}
