package com.pbrane.mike.ipv4subnet;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// todo:
//  learn and inmplement IPv6 subnetting
//
// filtered source addresses
// 0/8                 ! broadcast
// 10/8                ! RFC 1918 private
// 127/8               ! loopback
// 169.254.0/16        ! link local
// 172.16.0.0/12       ! RFC 1918 private
// 192.0.2.0/24        ! TEST-NET
// 192.168.0/16        ! RFC 1918 private
// 224.0.0.0/4         ! class D multicast
// 240.0.0.0/5         ! class E reserved
// 248.0.0.0/5         ! reserved
// 255.255.255.255/32  ! broadcast

public class CalculateSubnet {

    private static final String IP_ADDRESS = "(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])";
    private static final String CIDR = IP_ADDRESS + "(/([0-9]|[1-2][0-9]|3[0-2]))";
    private static final Pattern addressPattern = Pattern.compile("^" + IP_ADDRESS + "$");
    private static final Pattern cidrPattern = Pattern.compile("^" + CIDR + "$");

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

    public void calculateSubnetCIDR(String ipAddr_mask)
    {
        String []tmp = ipAddr_mask.split("/"); // split the IP and mask bits
        ipAddr = tmp[0];
        network_bits = Integer.parseInt(tmp[1]);
        host_bits = 32 - network_bits;
        binary_mask = maskBitsToBinary(network_bits); // netmask as binary string

        hosts_per_subnet = calcHostsPerSubnet(); // number of addresses (hosts) in subnet
        usable_hosts = calcUsableHosts(); // number of usable host IPs in network
        network = bitwiseAnd(ipAddr, splitIntoDecimalOctets(binary_mask)); // network IP

        min_host_addr = minimumHostAddress(); // first host IP
        broadcast = broadcast(ipAddr, splitIntoDecimalOctets(binary_mask)); // broadcast IP
        max_host_addr = maximumHostAddress(); // last host IP
		available_subnets = calcAvailableSubnets(); // available networks in this subnet
		// calculate all subnet ranges if the number of subnets is less than MAX_RANGES
		if (available_subnets <= MainActivity.MAX_RANGES) {
			ranges = calculateNetworkRanges();
		} else { // calculate just the host network ranges
			ranges = calculateRangeOfHostNetwork();
		}
    }

    public boolean validateCIDR(String cidr)
    {
        Matcher matcher = cidrPattern.matcher(cidr);
        return matcher.matches();
    }

    public boolean validateIPAddress(String ip)
    {
        Matcher matcher = addressPattern.matcher(ip);
        return matcher.matches();
    }

    public boolean validateIPAndMaskOctets(String ip)
    {
        String[] tmp = ip.split("\\s+");
		return tmp.length >= 2 && (validateIPAddress(tmp[0]) && validateIPAddress(tmp[1]));
	}

	public String convertToCIDR(String ipAndMask)
	{
		String[] tmp = ipAndMask.split("\\s+");
		long net_bits = Long.bitCount(Long.parseLong(ipToDecimal(tmp[1])));
		return tmp[0] + "/" + Long.toString(net_bits);
	}

    public String getWildcard()
    {
        return bitwiseInvert(splitIntoDecimalOctets(binary_mask));
    }

//	public static boolean isInRange(long x, long lower, long upper) {
//		return lower <= x && x <= upper;
//	}

    public long calcHostsPerSubnet()
    {
		long hosts = (long)Math.pow(2, (double)host_bits);
		if (hosts < 0) {
			return 1;
		}
		return hosts;
    }

    public long calcUsableHosts()
    {
		long total = calcHostsPerSubnet();
		long usable = total - 2;
		if (total == 1) {
			return 1;
		} else if (usable < 0) {
			return 0;
		}
		return usable;
    }

	public String getNetworkClass(String ip)
	{
		if (isClassA(ip)) {
			return "A";
		} else if (isClassB(ip)) {
			return "B";
		} else if (isClassC(ip)) {
			return "C";
		} else if (isClassD(ip)) {
			return "D";
		} else if (isClassE(ip)) {
			return "E";
		} else {
			return "unknown";
		}
	}

	public String getNetworkClassMask(String network)
	{
		switch (getNetworkClass(network)) {
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

	public String getNetworkClassBroadcast(String network)
	{
		return broadcast(network, getNetworkClassMask(network));
	}

	//
	// Private IP ranges:
	// Class A: 10.0.0.0 - 10.255.255.255
	// Class B: 172.16.0.0 - 172.31.255.255
	// Class C: 192.168.0.0 - 192.168.255.255
	//
	public boolean isPrivateIP(String ip)
	{
		long ipDec = Long.parseLong(ipToDecimal(ip));
		long ipLow = 0;
		long ipHigh = 0;
		if (isClassA(ip)) {
			ipLow = Long.parseLong(ipToDecimal("10.0.0.0"));
			ipHigh = Long.parseLong(ipToDecimal("10.255.255.255"));
		} else if (isClassB(ip)) {
			ipLow = Long.parseLong(ipToDecimal("172.16.0.0"));
			ipHigh = Long.parseLong(ipToDecimal("172.31.255.255"));
		} else if (isClassC(ip)) {
			ipLow = Long.parseLong(ipToDecimal("192.168.0.0"));
			ipHigh = Long.parseLong(ipToDecimal("192.168.255.255"));
		}
		return ipDec >= ipLow && ipDec <= ipHigh;
	}

	public boolean isLoopBackOrDiagIP(String ip)
	{
		long ipDec = Long.parseLong(ipToDecimal(ip));
		long ipLow = Long.parseLong(ipToDecimal("127.0.0.0"));
		long ipHigh = Long.parseLong(ipToDecimal("127.255.255.255"));
		return ipDec >= ipLow && ipDec <= ipHigh;
	}

	// Class A 0.0.0.0 to 127.255.255.255, 127.0.0.0 to 127.255.255.255 cannot be used and is
	// reserved for loopback and diagnostic functions
	public boolean isClassA(String ip)
	{
		return octetToBinary(ip.split("[.]")[0]).startsWith("0");
	}

	// Class B 128.0.0.0 to 191.255.255.255, 172.16.0.0 - 172.31.255.255 are private
	public boolean isClassB(String ip)
	{
		return octetToBinary(ip.split("[.]")[0]).startsWith("10");
	}

	// Class C 192.0.0.0 to 223.255.255.255
	public boolean isClassC(String ip)
	{
		return octetToBinary(ip.split("[.]")[0]).startsWith("110");
	}

	// Class D is a multicast network 224.0 0 0 to 239.255.255.255
	public boolean isClassD(String ip)
	{
		return octetToBinary(ip.split("[.]")[0]).startsWith("1110");
	}

	// class E is a reserved network 240.0.0.0 255.255.255.255
	public boolean isClassE(String ip)
	{
		return octetToBinary(ip.split("[.]")[0]).startsWith("1111");
	}

	// Calculates subnets from a valid mask. _Not_ CIDR or VLSM
	public int calcAvailableSubnets()
	{
		String[] octets = getDecimalMaskOctets().split("[.]");
		long hostbits = 0; // host bits of subnet mask

		switch (getNetworkClass(ipAddr)) {
			case "A": // n = 8, h = 24 255.0.0.0
				hostbits = Long.bitCount(Long.parseLong(octets[1]));
				hostbits += Long.bitCount(Long.parseLong(octets[2]));
				hostbits += Long.bitCount(Long.parseLong(octets[3]));
				break;

			case "B": // n = 16, h = 16 255.255.0.0
				hostbits = Long.bitCount(Long.parseLong(octets[2]));
				hostbits += Long.bitCount(Long.parseLong(octets[3]));
				break;

			case "C": // n = 24, h = 8 255.255.255.0
				hostbits = Long.bitCount(Long.parseLong(octets[3]));
				break;
		}
		return (int)Math.pow(2.0, (double)hostbits);
	}

	public String getNextNetwork(String base, int incremental_value)
	{
		String[] net = base.split("[:]"); // split the base network range at the colon
		String[] octets = net[0].split("[.]"); // split the ip
		if (octets.length < 4) {
			Log.i("getNextNetwork", "octets.length is less than four!");
			return "";
		}
		if (host_bits <= 8) { // class C
			octets[3] = Integer.toString(Integer.parseInt(octets[3]) + incremental_value);
		} else if (host_bits > 8 && host_bits <= 16) { // class B
			octets[3] = Integer.toString((incremental_value == hosts_per_subnet - 1) ? 255 : 0);
			octets[2] = Integer.toString(Integer.parseInt(octets[2]) + (incremental_value / 256));
		} else if (host_bits > 16 && host_bits <= 24) { // class A
			int octet3 = net.length > 1 ? Integer.parseInt(net[1].split("[.]")[3]) : Integer.parseInt(octets[3]);
			int octet2 = net.length > 1 ? Integer.parseInt(net[1].split("[.]")[2]) : Integer.parseInt(octets[2]);
			octets[3] = Integer.toString(octet3 == 255 ? 0 : 255);
			octets[2] = Integer.toString(octet2 == 255 ? 0 : 255);
			octets[1] = Integer.toString(Integer.parseInt(octets[1]) + (incremental_value / 256));
		}
		return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
	}

	public String getBaseNetwork(String base)
	{
		String[] octets = base.split("[.]");
		if (octets.length < 4) {
			Log.i("getBaseNetwork", "octets.length is less than four!");
			return "";
		}
		switch (getNetworkClass(base)) {
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

	public String[] calculateNetworkRanges()
	{
		int nets = available_subnets > MainActivity.MAX_RANGES ? MainActivity.MAX_RANGES : available_subnets;
		String[] networks = new String[nets];
		String base, top;

		if (available_subnets == 1) {
			base = getBaseNetwork(network);
			top = getBroadcast();
			networks[0] = base + " - " + top;
		} else {
			base = getBaseNetwork(network);
			if (!base.isEmpty()) {
				networks[0] = base + " - " + getNextNetwork(base, (int) hosts_per_subnet - 1);
				for (int k = 1; k < nets; k++) {
					base = getNextNetwork(base, (int) hosts_per_subnet);
					top = getNextNetwork(base, (int) hosts_per_subnet - 1);
					networks[k] = base + " - " + top;
				}
			}
		}
		return networks;
	}

	public String[] calculateRangeOfHostNetwork()
	{
		int nets = (int)(256 / hosts_per_subnet);
		String[] networks = new String[nets];

		String base = network.split("[.]")[0] + "." + network.split("[.]")[1] + "." + network.split("[.]")[2] + ".0";
		if (!base.isEmpty()) {
			networks[0] = base + " - " + getNextNetwork(base, (int)hosts_per_subnet - 1);
			for (int k = 1; k < nets; k++) {
				base = getNextNetwork(base, (int)hosts_per_subnet);
				String top = getNextNetwork(base, (int)hosts_per_subnet - 1);
				networks[k] = base + " - " + top;
			}
		}
		return networks;
	}

	public long calcHostsForNetworkClass(String network)
	{
		long result;
		switch (getNetworkClass(network)) {
			case "A":
				result = (long)Math.pow(2.0, 24.0);
				break;

			case "B":
				result = (long)Math.pow(2.0, 16.0);
				break;

			case "C":
				result = (long)Math.pow(2.0, 8.0);
				break;

			default:
				result = 0;
				break;
		}
		return result;
	}

    public String ipToHex(String ip)
    {
        String[] octets = ip.split("[.]");
		String hex = "";

		for (int k = 0; k < 4; k++) {
			hex += octetToHex(octets[k]);
		}
        return hex.toUpperCase();
    }

	public String ipToDecimal(String ipAddress)
	{
		String[] octets = ipAddress.split("\\.");

		long result = 0;
		for (int i = 0; i < octets.length; i++) {
			int power = 3 - i;
			result += Integer.parseInt(octets[i]) * Math.pow(256, power);
		}

		return Long.toString(result);
	}

	public String ipToBinary(String ip, boolean split)
	{
		String[] octets = ip.split("[.]");
		String binary = "";

		for (int k = 0; k < 4; k++) {
			binary += octetToBinary(octets[k]);
			if (split && k != 3) {
				binary += ".";
			}
		}
		return binary;
	}

    public String maskBitsToBinary(int bits)
    {
        char[] b = "00000000000000000000000000000000".toCharArray();
        for (int i = 0; i < bits; i++) {
            b[i] = '1';
        }
        return new String(b);
    }

    public String splitIntoDecimalOctets(String b)
    {
        return Integer.toString(Integer.parseInt(b.substring(0, 8), 2)) + "."
                + Integer.toString(Integer.parseInt(b.substring(8, 16), 2)) + "."
                + Integer.toString(Integer.parseInt(b.substring(16, 24), 2)) + "."
                + Integer.toString(Integer.parseInt(b.substring(24, 32), 2));
    }

	public String octetToBinary(String octet)
	{
		String padding = "00000000"; // 8 bits in an octet
		String result = padding + Integer.toBinaryString(Integer.parseInt(octet));
		return result.substring(result.length() - padding.length(), result.length());
	}

	public String octetToHex(String octet)
	{
		String padding = "00"; // 2 hex characters in an octet
		String result = padding + Integer.toHexString(Integer.parseInt(octet));
		return result.substring(result.length() - padding.length(), result.length());
	}

	public String broadcast(String network, String netmask)
	{
		// bitwise OR the network IP and the inverted netmask (hostmask)
		return bitwiseOr(network, bitwiseInvert(netmask));
	}

	// The network plus one
    public String minimumHostAddress()
    {
        String[] octets = network.split("[.]");

        octets[3] = Integer.toString(Integer.parseInt(octets[3]) + 1);
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

	// the broadcast minus one
    public String maximumHostAddress()
    {
        String[] octets = broadcast.split("[.]");

        octets[3] = Integer.toString(Integer.parseInt(octets[3]) - 1);
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

    public String bitwiseAnd(String ip1, String ip2) {
        String[] inputA = ip1.split("[.]");
        String[] inputB = ip2.split("[.]");
        String[] output = new String[4];

        for (int i = 0; i < 4; i++) {
            output[i] = Integer.toString(Integer.parseInt(inputA[i]) & Integer.parseInt(inputB[i]) );
        }
        return output[0] + "." + output[1] + "." + output[2] + "." + output[3];
    }

	public String bitwiseOr(String ip1, String ip2)
	{
		String[] inputA = ip1.split("[.]");
		String[] inputB = ip2.split("[.]");
		String[] output = new String[4];

		for (int i = 0; i < 4; i++) {
			output[i] = Integer.toString(Integer.parseInt(inputA[i]) | Integer.parseInt(inputB[i]) );
		}
		return output[0] + "." + output[1] + "." + output[2] + "." + output[3];
	}

    public String bitwiseInvert(String ip)
    {
        String[] input = ip.split("[.]");
        String[] output = new String[4];

        for (int k = 0; k < 4; k++) {
            output[k] = Integer.toString(255 - Integer.parseInt(input[k]));
        }
        return output[0] + "." + output[1] + "." + output[2] + "." + output[3];
    }

    public String getIpAddr()
    {
        return ipAddr;
    }

    public String getIpAddrHex()
    {
        return ipToHex(ipAddr);
    }

	public String getIpAddrDecimal()
	{
		return ipToDecimal(ipAddr);
	}

    public int getNetworkBits()
    {
        return network_bits;
    }

    public String getDecimalMaskOctets()
    {
        return splitIntoDecimalOctets(binary_mask);
    }

    public String getBroadcast()
    {
        return broadcast;
    }

    public String getMinHostAddr()
    {
        return min_host_addr;
    }

    public String getMaxHostAddr()
    {
        return max_host_addr;
    }

    public String getNetwork()
    {
        return network;
    }

    public long getUsableHosts()
    {
        return usable_hosts;
    }

    public long getNumberOfAddresses()
    {
        return hosts_per_subnet;
    }

	public int getHostBits()
	{
		return host_bits;
	}

	public String[] getRanges()
	{
		return ranges;
	}

	public int getAvailableSubnets()
	{
		return available_subnets;
	}

}
