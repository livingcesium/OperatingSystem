import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

public class VirtualFileSystem implements Device {
    
    // Logger for debugging
    public static final boolean LOGGING = true;

    private class Logger {
        private final StringBuilder operationsLog = new StringBuilder();
        private final StringBuilder devicesTable = new StringBuilder();

        public void logOperation(String operation) {
            if(!LOGGING)
                return;
            
            synchronized (operationsLog) {
                operationsLog.append(operation).append("\n");
            }
        }

        public void updateDevicesTable() {
            if(!LOGGING)
                return;
            
            synchronized (devicesTable) {
                devicesTable.setLength(0);  // Clear the previous table
                devicesTable.append("Device ID Table:\n");
                for (int i = 0; i < deviceIds.devices.length; i++) {
                    if (deviceIds.getDevice(i) != null) {
                        devicesTable.append("Index: ").append(i)
                                .append(", Inner ID: ").append(deviceIds.getId(i))
                                .append(", Device: ").append(deviceIds.getDevice(i).getClass().getSimpleName())
                                .append("\n");
                    }
                }
                
            }
        }

        public String getReport() {
            if(!LOGGING)
                throw new RuntimeException("Logging is disabled");
            return devicesTable.toString() + "\n" + operationsLog.toString();
        }
    }

    private final Logger logger = new Logger();

    private class DeviceIdBank {

        public static HashMap<String, Class<? extends Device>> deviceNameToClass;

        static { // Static block will populate the map once the class is loaded
            deviceNameToClass = new HashMap<>();
            deviceNameToClass.put("random", RandomDevice.class);
            deviceNameToClass.put("file", FakeFileSystem.class);

        }

        private final Device[] devices;
        private final int[] ids;

        DeviceIdBank(int size) {
            devices = new Device[size];
            ids = new int[size];
            Arrays.fill(ids, -1);
        }

        public int addDevice(Device device, int id) {
            for (int i = 0; i < devices.length; i++) {
                if (devices[i] == null) {
                    devices[i] = device;
                    ids[i] = id;
                    return i;
                }
            }
            return -1;
        }

        public void removeDevice(int i) {
            devices[i] = null;
            ids[i] = -1;
        }

        public Device getDevice(int i) {
            return devices[i];
        }

        public int getId(int i) {
            return ids[i];
        }
        
        public boolean inRange(int i){
            return i >= 0 && i < devices.length;
        }

    }

    private static DeviceIdBank deviceIds;

    public VirtualFileSystem(int maxDevices) {
        deviceIds = new DeviceIdBank(maxDevices);
    }

    @Override
    public int open(String s) throws FileNotFoundException {
        String[] words = s.split(" ");
        boolean noArg = words.length < 2;

        String deviceName = words[0]; // Associate device name to class
        Class<? extends Device> deviceClass;
        if (!DeviceIdBank.deviceNameToClass.containsKey(deviceName))
            throw new FileNotFoundException("No such device");
        deviceClass = DeviceIdBank.deviceNameToClass.get(deviceName);

        if (noArg && deviceClass == FakeFileSystem.class) 
            throw new FileNotFoundException("Expected filename");

        Device device;

        // Instantiate the device. It's a lot less pretty than "new Device", but it's kind of neat
        try {
            device = deviceClass.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate device: " + deviceName, e);
            // Fail if:
            // 1. The device doesn't have a no-args constructor
            // 2. The class is abstract, interface, or array (not instantiatable)
            // 3. Access to the class constructor is denied (it's private)
            // 4. The constructor threw an exception while instantiating
            // Pretty much like how java normally does it.
        }
        
        
        int innerId, resultId;
        innerId = noArg ? device.open(null) : device.open(words[1]);
        if (innerId == -1)
            throw new FileNotFoundException(String.format("Failed to open device (%s)", deviceClass.getSimpleName()));
        
        resultId = deviceIds.addDevice(device, innerId);
        logger.logOperation(String.format("Opened device (%s) with innerID %d, at id %d", deviceClass.getSimpleName(), innerId, resultId));
        logger.updateDevicesTable();
        return resultId;
    }

    @Override
    public void close(int id) throws IOException {
        if(!deviceIds.inRange(id))
            throw new IOException(String.format("id %d outside of range", id));
        Device device = deviceIds.getDevice(id);
        if (device != null) {
            int innerId;
            device.close(innerId = deviceIds.getId(id));
            deviceIds.removeDevice(id);
            
            logger.logOperation(String.format("Closed device (%s) with innerID %d, at id %d", device.getClass().getSimpleName(),innerId, id));
            logger.updateDevicesTable();
            
        } else throw new IOException(String.format("No device with id %d", id));
    }

    public void close(String s) throws IOException {
        // Closes first occurrence of device with name s
        
        Device device;
        if (!DeviceIdBank.deviceNameToClass.containsKey(s))
            throw new IOException(String.format("No device with name %s", s));
        Class<? extends Device> deviceClass = DeviceIdBank.deviceNameToClass.get(s);
        for (int i = 0; i < deviceIds.devices.length; i++) {
            if (deviceClass.isInstance(device = deviceIds.getDevice(i))) {
                int innerId;
                device.close(innerId = deviceIds.getId(i));
                logger.logOperation(String.format("Closing first occurrence of device '%s'. Found with innerID %d, at id %d", s, innerId, i));
                logger.updateDevicesTable();
                break;
            }
        }
    }

    @Override
    public byte[] read(int id, int size) throws IOException {
        if(!deviceIds.inRange(id))
            throw new IOException(String.format("id %d outside of range", id));
        
        Device device = deviceIds.getDevice(id);
        if (device != null){
            logger.logOperation(String.format("Reading %d bytes from device (%s) at id %d", size, device.getClass().getSimpleName(), id));
            return device.read(deviceIds.getId(id), size);
        }
        else throw new IOException(String.format("No device with id %d", id));
    }

    @Override
    public void seek(int id, int to) throws IOException {
        if(!deviceIds.inRange(id))
            throw new IOException(String.format("id %d outside of range", id));
        
        Device device = deviceIds.getDevice(id);
        if (device != null) {
            logger.logOperation(String.format("Seeking to %d on device (%s) at id %d", to, device.getClass().getSimpleName(), id));
            device.seek(deviceIds.getId(id), to);
        }
        else throw new IOException(String.format("No device with id %d", id));
    }

    @Override
    public int write(int id, byte[] data) throws IOException {
        if(!deviceIds.inRange(id))
            throw new IOException(String.format("id %d outside of range", id));
        
        Device device = deviceIds.getDevice(id);
        
        if (device != null) {
            logger.logOperation(String.format("Writing %d bytes to device (%s) at id %d", data.length, device.getClass().getSimpleName(), id));
            return device.write(deviceIds.getId(id), data);
        } else 
            throw new IOException(String.format("No device with id %d", id));
    }
    
    public String getLog(){
        return logger.getReport();
    }
}
