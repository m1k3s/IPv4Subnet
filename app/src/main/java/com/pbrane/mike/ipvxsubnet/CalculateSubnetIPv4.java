package com.pbrane.mike.ipvxsubnet;

import android.util.Log;

import java.util.regex.Pattern;


class CalculateSubnetIPv4 {

    private static final int CLASS_A_HOSTS = 16777216;
    private static final int CLASS_B_HOSTS = 65536;
    private static final int CLASS_C_HOSTS = 256;
    private static final int IPV4_ADDR_BITS = 32;
    private static final int MAX_RANGES = 32; // maximum count of network ranges to display
    private static final String IP_ADDRESS = "(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])";
    private static final String CIDR = IP_ADDRESS + "(/([0-9]|[1-2][0-9]|3[0-2]))";
    private static final Pattern addressPattern = Pattern.compile ("^" + IP_ADDRESS + "$");
    private static final Pattern cidrPattern = Pattern.compile ("^" + CIDR + "$");

    private String ipAddr;
    private int network_bits;
    private int host_bits;
    private String binary_mask;
    private String broadcast;
    private String min_host_addr;
    private String max_host_addr;
    private String network;
    private long hosts_per_subnet;
    private long usable_hosts;
    private int available_subnets;
    private String[] ranges;

    final static CalculateSubnetIPv4 INSTANCE = new CalculateSubnetIPv4();

    private CalculateSubnetIPv4() { // avoids instantiation

    }

    boolean calculateSubnetCIDR(String ipAddr_mask) {
        String[] tmp = ipAddr_mask.split ("/"); // split the IP and mask bits
        if (tmp.length < 2) {  // make sure the split was successful
            return false;
        }
        ipAddr = tmp[0];
        try {
            network_bits = Integer.parseInt (tmp[1]);
        } catch (NumberFormatException e) {
            Log.e ("calculateSubnetCIDR", "NumberFormatException");
            return false;
        }
        host_bits = IPV4_ADDR_BITS - network_bits;
        binary_mask = maskBitsToBinary (network_bits); // netmask as binary string

        hosts_per_subnet = calcHostsPerSubnet(); // number of addresses (hosts) in subnet
        usable_hosts = calcUsableHosts(); // number of usable host IPs in network

        network = bitwiseAnd (ipAddr, splitIntoDecimalOctets (binary_mask) ); // network IP
        min_host_addr = minimumHostAddress(); // first host IP
        broadcast = broadcast (ipAddr, splitIntoDecimalOctets (binary_mask) ); // broadcast IP
        max_host_addr = maximumHostAddress(); // last host IP

        available_subnets = calcAvailableSubnets(); // available networks in this subnet

        // calculate all subnet ranges if the number of subnets is less than MAX_RANGES
        if (available_subnets <= MAX_RANGES) {
            ranges = calculateNetworkRanges();
        } else { // calculate just the host network ranges
            ranges = calculateRangeOfHostNetwork();
        }
        return true;
    }

    boolean validateCIDR(String cidr) {
        return cidrPattern.matcher (cidr).matches();
    }

    boolean validateIPAddress(String ip) {
        return addressPattern.matcher (ip).matches();
    }

    boolean validateIPAndMaskOctets(String ip) {
        String[] tmp = ip.split ("\\s+");
        return tmp.length >= 2 && (validateIPAddress (tmp[0]) && validateIPAddress (tmp[1]) );
    }

    String convertToCIDR(String ipAndMask) {
        String[] tmp = ipAndMask.split ("\\s+");
        if (tmp.length != 2) {
            return ipAndMask;
        }
        long net_bits;
        try {
            net_bits = Long.bitCount (Long.parseLong (ipToDecimal (tmp[1]) ) );
        } catch (NumberFormatException e) {
            Log.e ("convertToCIDR", "NumberFormatException");
            return ipAndMask;
        }
        return tmp[0] + "/" + Long.toString (net_bits);
    }

    String getWildcard() {
        return bitwiseInvert (splitIntoDecimalOctets (binary_mask) );
    }

    private long calcHostsPerSubnet() {
        long hosts = (long) Math.pow (2, (double) host_bits);
        return hosts < 0 ? 1 : hosts;
    }

    private long calcUsableHosts() {
        long total = calcHostsPerSubnet();
        long usable = total - 2;
        if (total == 1) {
            usable = 1;
        } else if (usable < 0) {
            usable = 0;
        }
        return usable;
    }

    String getNetworkClass(String ip) {
        if (isClassA (ip) ) {
            return "A";
        } else if (isClassB (ip) ) {
            return "B";
        } else if (isClassC (ip) ) {
            return "C";
        } else if (isClassD (ip) ) {
            return "D";
        } else if (isClassE (ip) ) {
            return "E";
        } else {
            return "unknown";
        }
    }

    String getNetworkClassMask(String network) {
        switch (getNetworkClass (network) ) {
            case "A": // n = 8, h = 24 255.0.0.0
                return "255.0.0.0";

            case "B": // n = 16, h = 16 255.255.0.0
                return "255.255.0.0";

            case "C": // n = 24, h = 8 255.255.255.0
                return "255.255.255.0";

            default:
                return "unknown";
        }
    }

    String getNetworkClassBroadcast(String network) {
        return broadcast (network, getNetworkClassMask (network) );
    }

    //
    // Private IP ranges:
    // Class A: 10.0.0.0 - 10.255.255.255      (10.0.0.0/8)
    // Class B: 172.16.0.0 - 172.31.255.255    (172.16.0.0/12)
    // Class C: 192.168.0.0 - 192.168.255.255  (192.168.0.0/16)
    //
    boolean isPrivateIP(String ip) {
        long ipDec, ipLow = 0, ipHigh = 0;
        try {
            ipDec = Long.parseLong (ipToDecimal (ip) );
        } catch (NumberFormatException e) {
            return false;
        }
        if (isClassA (ip) ) {
            try {
                ipLow = Long.parseLong (ipToDecimal ("10.0.0.0") );
                ipHigh = Long.parseLong (ipToDecimal ("10.255.255.255") );
            } catch (NumberFormatException e) {
                Log.e ("isPrivateIP", "NumberFormatException");
                return false;
            }
        } else if (isClassB (ip) ) {
            try {
                ipLow = Long.parseLong (ipToDecimal ("172.16.0.0") );
                ipHigh = Long.parseLong (ipToDecimal ("172.31.255.255") );
            } catch (NumberFormatException e) {
                Log.e ("isPrivateIP", "NumberFormatException");
                return false;
            }
        } else if (isClassC (ip) ) {
            try {
                ipLow = Long.parseLong (ipToDecimal ("192.168.0.0") );
                ipHigh = Long.parseLong (ipToDecimal ("192.168.255.255") );
            } catch (NumberFormatException e) {
                Log.e ("isPrivateIP", "NumberFormatException");
                return false;
            }
        }
        return ipDec >= ipLow && ipDec <= ipHigh;
    }

    boolean isLoopBackOrDiagIP(String ip) {
        long ipDec, ipLow, ipHigh;
        try {
            ipDec = Long.parseLong (ipToDecimal (ip) );
            ipLow = Long.parseLong (ipToDecimal ("127.0.0.0") );
            ipHigh = Long.parseLong (ipToDecimal ("127.255.255.255") );
        } catch (NumberFormatException e) {
            Log.e ("isLoopBackOrDiagIP", "NumberFormatException");
            return false;
        }
        return ipDec >= ipLow && ipDec <= ipHigh;
    }

    // Class A 0.0.0.0 to 127.255.255.255, 127.0.0.0 to 127.255.255.255 cannot be used and is
    // reserved for loopback and diagnostic functions
    boolean isClassA(String ip) {
        String[] octets = ip.split ("[.]");
        return octets.length > 0 && (octetToBinary (octets[0]).startsWith ("0") );
    }

    // Class B 128.0.0.0 to 191.255.255.255, 172.16.0.0 - 172.31.255.255 are private
    boolean isClassB(String ip) {
        String[] octets = ip.split ("[.]");
        return octets.length > 0 && (octetToBinary (octets[0]).startsWith ("10") );
    }

    // Class C 192.0.0.0 to 223.255.255.255, 192.168.0.0 - 192.168.255.255 are private
    boolean isClassC(String ip) {
        String[] octets = ip.split ("[.]");
        return octets.length > 0 && (octetToBinary (octets[0]).startsWith ("110") );
    }

    // Class D is a multicast network 224.0 0 0 to 239.255.255.255
    boolean isClassD(String ip) {
        String[] octets = ip.split ("[.]");
        return octets.length > 0 && (octetToBinary (octets[0]).startsWith ("1110") );
    }

    // class E is a reserved network 240.0.0.0 255.255.255.255
    boolean isClassE(String ip) {
        String[] octets = ip.split ("[.]");
        return octets.length > 0 && (octetToBinary (octets[0]).startsWith ("1111") );
    }

    // Calculates subnets from a valid mask. _Not_ CIDR or VLSM
    private int calcAvailableSubnets() {
        String[] octets = getDecimalMaskOctets().split ("[.]");
        long hostbits = 0; // host bits of subnet mask

        try {
            switch (getNetworkClass (ipAddr) ) {
                case "A": // n = 8, h = 24 255.0.0.0
                    hostbits = Long.bitCount (Long.parseLong (octets[1]) );
                    hostbits += Long.bitCount (Long.parseLong (octets[2]) );
                    hostbits += Long.bitCount (Long.parseLong (octets[3]) );
                    break;

                case "B": // n = 16, h = 16 255.255.0.0
                    hostbits = Long.bitCount (Long.parseLong (octets[2]) );
                    hostbits += Long.bitCount (Long.parseLong (octets[3]) );
                    break;

                case "C": // n = 24, h = 8 255.255.255.0
                    hostbits = Long.bitCount (Long.parseLong (octets[3]) );
                    break;
            }
        } catch (NumberFormatException e) {
            Log.e ("calcAvailableSubNets", "NumberFormatException");
            hostbits = 0;
        }
        return (int) Math.pow (2.0, (double) hostbits);
    }

    String getNextIPAddress(String ip) {
        String[] octets = ip.split ("[.]");
        if (octets.length < 4) {
            Log.e ("getNextIPAddress", "octets.length is less than four!");
            return ip;
        }
        try {
            octets[3] = Integer.toString (Integer.parseInt (octets[3]) + 1);
        } catch (NumberFormatException e) {
            Log.e ("getNextIPAddress", "NumberFormatException");
            return ip;
        }
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

    private String getNextNetwork(String base, int incremental_value) {
        String[] octets = base.split ("[.]"); // split the ip
        if (octets.length < 4) {
            Log.e ("getNextNetwork", "octets.length is less than four!");
            return base;
        }
        try {
            if (host_bits <= 8) { // class C
                octets[3] = Integer.toString (Integer.parseInt (octets[3]) + incremental_value);
            } else if (host_bits > 8 && host_bits <= 16) { // class B
                octets[3] = Integer.toString ( (incremental_value == hosts_per_subnet - 1) ? 255 : 0);
                octets[2] = Integer.toString (Integer.parseInt (octets[2]) + (incremental_value / 256) );
            } else if (host_bits > 16 && host_bits <= 24) { // class A
                int octet3 = Integer.parseInt (octets[3]);
                int octet2 = Integer.parseInt (octets[2]);
                octets[3] = Integer.toString (octet3 == 255 ? 0 : 255);
                octets[2] = Integer.toString (octet2 == 255 ? 0 : 255);
                octets[1] = Integer.toString (Integer.parseInt (octets[1]) + (incremental_value / 256) );
            }
        } catch (NumberFormatException e) {
            Log.e ("getNextNetwork", "NumberFormatException");
            return base;
        }
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

    String getBaseNetwork(String base) {
        String[] octets = base.split ("[.]");
        if (octets.length < 4) {
            Log.e ("getBaseNetwork", "octets.length is less than four!");
            return base;
        }
        switch (getNetworkClass (base) ) {
            case "A":
                octets[3] = "0";
                octets[2] = "0";
                octets[1] = "0";
                break;

            case "B":
                octets[3] = "0";
                octets[2] = "0";
                break;

            case "C":
                octets[3] = "0";
                break;
        }
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

    private String[] calculateNetworkRanges() {
        int nets = available_subnets > MAX_RANGES ? MAX_RANGES : available_subnets;
        String[] networks = new String[nets];
        String base, top;

        if (available_subnets == 1) {
            base = getBaseNetwork (network);
            top = getBroadcast();
            networks[0] = base + " - " + top;
        } else {
            base = getBaseNetwork (network);
            if (!base.isEmpty() ) {
                networks[0] = base + " - " + getNextNetwork (base, (int) hosts_per_subnet - 1);
                for (int k = 1; k < nets; k++) {
                    base = getNextNetwork (base, (int) hosts_per_subnet);
                    top = getNextNetwork (base, (int) hosts_per_subnet - 1);
                    networks[k] = base + " - " + top;
                }
            }
        }
        return networks;
    }

    private String[] calculateRangeOfHostNetwork() {
        int nets = calcNetworksInSubnet();
        String[] networks = new String[nets];

        String base = network.split ("[.]") [0] + "." + network.split ("[.]") [1] + "." + network.split ("[.]") [2] + ".0";
        if (nets == 1) {
            networks[0] = network + " - " + getBroadcast();
        } else {
            if (!base.isEmpty() ) {
                networks[0] = base + " - " + getNextNetwork (base, (int) hosts_per_subnet - 1);
                for (int k = 1; k < nets; k++) {
                    base = getNextNetwork (base, (int) hosts_per_subnet);
                    String top = getNextNetwork (base, (int) hosts_per_subnet - 1);
                    networks[k] = base + " - " + top;
                }
            }
        }
        return networks;
    }

    private int calcNetworksInSubnet() {
        int nets;
        if (hosts_per_subnet > CLASS_B_HOSTS) {
            nets = (int) (CLASS_A_HOSTS / hosts_per_subnet);
        } else if (hosts_per_subnet > CLASS_C_HOSTS && hosts_per_subnet <= CLASS_B_HOSTS) {
            nets = (int) (CLASS_B_HOSTS / hosts_per_subnet);
        } else { // hosts_per_subnet <= CLASS_C_HOSTS
            nets = (int) (CLASS_C_HOSTS / hosts_per_subnet);
        }
        return nets <= 0 ? 1 : nets;
    }

    long calcHostsForNetworkClass(String network) {
        long result;
        switch (getNetworkClass (network) ) {
            case "A":
                result = (long) Math.pow (2.0, 24.0);
                break;

            case "B":
                result = (long) Math.pow (2.0, 16.0);
                break;

            case "C":
                result = (long) Math.pow (2.0, 8.0);
                break;

            default:
                result = 0;
                break;
        }
        return result;
    }

    String ipToHex(String ip) {
        String[] octets = ip.split ("[.]");
        String hex = "";

        for (int k = 0; k < 4; k++) {
            hex += octetToHex (octets[k]);
        }
        return hex.toUpperCase();
    }

    String ipToDecimal(String ipAddress) {
        String[] octets = ipAddress.split ("\\.");

        long result = 0;
        for (int i = 0; i < octets.length; i++) {
            int power = 3 - i;
            result += Integer.parseInt (octets[i]) * Math.pow (256, power);
        }

        return Long.toString (result);
    }

    String ipToBinary(String ip, boolean split) {
        String[] octets = ip.split ("[.]");
        String binary = "";

        for (int k = 0; k < 4; k++) {
            binary += octetToBinary (octets[k]);
            if (split && k != 3) {
                binary += ".";
            }
        }
        return binary;
    }

    private String maskBitsToBinary(int bits) {
        char[] b = "00000000000000000000000000000000".toCharArray();
        for (int i = 0; i < bits; i++) {
            b[i] = '1';
        }
        return new String (b);
    }

    private String splitIntoDecimalOctets(String b) {
        return Integer.toString (Integer.parseInt (b.substring (0, 8), 2) ) + "."
               + Integer.toString (Integer.parseInt (b.substring (8, 16), 2) ) + "."
               + Integer.toString (Integer.parseInt (b.substring (16, 24), 2) ) + "."
               + Integer.toString (Integer.parseInt (b.substring (24, 32), 2) );
    }

    // leading zero pad the octet manually, toBinaryString doesn't
    private String octetToBinary(String octet) {
        String padding = "00000000"; // 8 bits in an octet
        String result = padding + Integer.toBinaryString (Integer.parseInt (octet) );
        return result.substring (result.length() - padding.length(), result.length() );
    }

    // leading zero pad the hex chars manually, toHexString doesn't
    private String octetToHex(String octet) {
        String padding = "00"; // 2 hex characters in an octet
        String result = padding + Integer.toHexString (Integer.parseInt (octet) );
        return result.substring (result.length() - padding.length(), result.length() );
    }

    private String broadcast(String network, String netmask) {
        // bitwise OR the network IP and the inverted netmask (hostmask)
        return bitwiseOr (network, bitwiseInvert (netmask) );
    }

    // The network plus one
    private String minimumHostAddress() {
        String[] octets = network.split ("[.]");

        octets[3] = Integer.toString (Integer.parseInt (octets[3]) + 1);
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

    // the broadcast minus one
    private String maximumHostAddress() {
        String[] octets = broadcast.split ("[.]");

        octets[3] = Integer.toString (Integer.parseInt (octets[3]) - 1);
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

    private String bitwiseAnd(String ip1, String ip2) {
        String[] inputA = ip1.split ("[.]");
        String[] inputB = ip2.split ("[.]");
        String[] output = new String[4];

        for (int i = 0; i < 4; i++) {
            output[i] = Integer.toString (Integer.parseInt (inputA[i]) & Integer.parseInt (inputB[i]) );
        }
        return output[0] + "." + output[1] + "." + output[2] + "." + output[3];
    }

    private String bitwiseOr(String ip1, String ip2) {
        String[] inputA = ip1.split ("[.]");
        String[] inputB = ip2.split ("[.]");
        String[] output = new String[4];

        for (int i = 0; i < 4; i++) {
            output[i] = Integer.toString (Integer.parseInt (inputA[i]) | Integer.parseInt (inputB[i]) );
        }
        return output[0] + "." + output[1] + "." + output[2] + "." + output[3];
    }

    private String bitwiseInvert(String ip) {
        String[] input = ip.split ("[.]");
        String[] output = new String[4];

        for (int k = 0; k < 4; k++) {
            output[k] = Integer.toString (255 - Integer.parseInt (input[k]) );
        }
        return output[0] + "." + output[1] + "." + output[2] + "." + output[3];
    }

    String getIpAddr() {
        return ipAddr;
    }

    String getIpAddrHex() {
        return ipToHex (ipAddr);
    }

    String getIpAddrDecimal() {
        return ipToDecimal (ipAddr);
    }

    int getNetworkBits() {
        return network_bits;
    }

    String getDecimalMaskOctets() {
        return splitIntoDecimalOctets (binary_mask);
    }

    String getBroadcast() {
        return broadcast;
    }

    String getMinHostAddr() {
        return min_host_addr;
    }

    String getMaxHostAddr() {
        return max_host_addr;
    }

    String getNetwork() {
        return network;
    }

    long getUsableHosts() {
        return usable_hosts;
    }

    long getNumberOfAddresses() {
        return hosts_per_subnet;
    }

    int getHostBits() {
        return host_bits;
    }

    String[] getRanges() {
        return ranges;
    }

    int getAvailableSubnets() {
        return available_subnets;
    }

}
