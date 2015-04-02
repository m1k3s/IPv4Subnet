package com.pbrane.mike.ipv4subnet;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private int hosts_per_subnet; // addresses in given subnet
    private int usable_hosts;
	private int available_subnets;
//	private String osm; // original subnet mask
//	private String csm; // custom subnet mask (osm + network bits)
//	private int number_of_networks;
//	private int number_of_hosts;
	private String[] ranges;// = new String[MainActivity.MAX_RANGES]; // max number of ranges is 32

    public void calculateSubnetCIDR(String ipAddr_mask)
    {
        String []tmp = ipAddr_mask.split("/"); // split the IP and mask bits
        ipAddr = tmp[0];
        network_bits = Integer.parseInt(tmp[1]);
        host_bits = 32 - network_bits;
        binary_mask = maskBitsToBinary(network_bits);

        // calc ip range
        hosts_per_subnet = calcHostsPerSubnet(); // number of addresses in subnet
        usable_hosts = calcUsableHosts(); // number of host IPs in network
        network = bitwiseAnd(ipAddr, splitIntoDecimalOctets(binary_mask)); // network IP

        min_host_addr = minimumHostAddress(network); // first host IP
        broadcast = broadcast(ipAddr, splitIntoDecimalOctets(binary_mask)); // broadcast IP
        max_host_addr = maximumHostAddress(broadcast); // last host IP
		available_subnets = calcAvailableSubnets();
		ranges = calculateNetworkRanges();
    }

	// subnetting steps ICND style
	//                        OSM
	// 1. ID the class A = 255.0.0.0 = 24 host bits
	//           Class B = 255.255.0.0 = 16 host bits
	//           Class C = 255.255.255.0 = 8 host bits
	//
	// 2.a) 2^xn >= # of networks, where xn = # of network bits
	//   b) 2^xh - 2 >= # of hosts/network, where xh = # of host bits
	//   c) x^n + x^h = # of available bits in octet
	//
	// 3. custom subnet mask (CSM) = OSM + x^n
	//
	// 4. use incremental value (IV) to determine the ranges of usable addresses
	// where IV = 1, 2, 4, 8, 16, 32, 64, 128 / octet value of 255, 254, 252, 248, 240, 224, 192, 128

//	public void calculateSubnet(String cidr)
//	{
//		String []tmp = cidr.split("/"); // split the IP and mask bits
//		ipAddr = tmp[0];
//		network_bits = Integer.parseInt(tmp[1]);
//
//		// nice stuff to know
//		binary_mask = maskBitsToBinary(network_bits);
////		network = bitwiseAnd(ipAddr, splitIntoDecimalOctets(binary_mask)); // network IP
//		broadcast = broadcast(ipAddr, splitIntoDecimalOctets(binary_mask)); // broadcast IP
////		min_host_addr = minimumHostAddress(network); // first host IP
//		max_host_addr = maximumHostAddress(broadcast); // last host IP
//
//		// step one
//		switch (getNetworkClass()) {
//			case "A":
//				host_bits = 24;
//				osm = "255.0.0.0";
//				break;
//
//			case "B":
//				host_bits = 16;
//				osm = "255.255.0.0";
//				break;
//
//			case "C":
//				host_bits = 8;
//				osm = "255.255.255.0";
//				break;
//		}
//		// step two
//		number_of_networks = (int)Math.pow(2.0, network_bits);
//		number_of_hosts = (int)Math.pow(2.0, host_bits);
//
//		// step three
//		csm = bitwiseOr(osm, splitIntoDecimalOctets(binary_mask));
//		network = bitwiseAnd(ipAddr, csm); // network IP
//		min_host_addr = minimumHostAddress(network); // first host IP
//
//		// step four
//		calculateNetworkRanges();
//	}

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

    // calculate the next IP
//    public String getNextIPAddress(String minIP)
//    {
//        String[] octets = minIP.split("[.]");
//
//		if (Integer.parseInt(octets[3]) < 255) {
//			octets[3] = Integer.toString(Integer.parseInt(octets[3]) + 1);
//		}
//		if (Integer.parseInt(octets[3]) == 255) {
//			if (Integer.parseInt(octets[2]) < 255) {
//				octets[2] = Integer.toString(Integer.parseInt(octets[2]) + 1);
//			}
//		}
//		if (Integer.parseInt(octets[2]) == 255) {
//			if (Integer.parseInt(octets[1]) < 255) {
//				octets[1] = Integer.toString(Integer.parseInt(octets[1]) + 1);
//			}
//		}
//		if (Integer.parseInt(octets[1]) == 255) {
//			if (Integer.parseInt(octets[0]) < 255) {
//				octets[0] = Integer.toString(Integer.parseInt(octets[0]) + 1);
//			}
//		}
//        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
//    }

    public int calcHostsPerSubnet()
    {
		if (host_bits < 2 ) {
			return 1;
		} else {
			return (int)Math.pow(2, (double)host_bits);
		}
    }

    public int calcUsableHosts()
    {
		if (host_bits < 2 ) {
			return 1;
		} else {
			return calcHostsPerSubnet() - 2;
		}
    }

	public String getNetworkClass()
	{
		String ip = octetToBinary(ipAddr.split("[.]")[0]);
		if (isClassA(ip)) {
			return "A";
		} else if (isClassB(ip)) {
			return "B";
		} else if (isClassC(ip)) {
			return "C";
		} else {
			return "unknown";
		}
	}

	public boolean isClassA(String ip)
	{
		return ip.split("[.]")[0].startsWith("0");
	}

	public boolean isClassB(String ip)
	{
		return ip.split("[.]")[0].startsWith("10");
	}

	public boolean isClassC(String ip)
	{
		return ip.split("[.]")[0].startsWith("110");
	}

	public int calcAvailableSubnets()
	{
		String ip = octetToBinary(ipAddr.split("[.]")[0]);
		Log.i("calcAvailableSubnets", ip);
		String[] octets = getDecimalMaskOctets().split("[.]");
		long hostbits = 0;

		if (isClassA(ip)) { // n = 8, h = 24 255.0.0.0
			hostbits = Long.bitCount(Long.parseLong(octets[1]));
			hostbits += Long.bitCount(Long.parseLong(octets[2]));
			hostbits += Long.bitCount(Long.parseLong(octets[3]));
		} else if (isClassB(ip)) { // n = 16, h = 16 255.255.0.0
			hostbits = Long.bitCount(Long.parseLong(octets[2]));
			hostbits += Long.bitCount(Long.parseLong(octets[3]));
		} else if (isClassC(ip)) { // n = 24, h = 8 255.255.255.0
			hostbits = Long.bitCount(Long.parseLong(octets[3]));
		}

		return (int)Math.pow(2.0, (double)hostbits);
	}

	public String getNextNetwork(String base, int incremental_value)
	{
		String[] net = base.split("[:]"); // split the base network range at the colon
		String[] octets = net[0].split("[.]"); // split the
		if (octets.length < 4) {
			Log.i("getNextNetwork", "octets.length is less than four!");
			return "";
		}
		if (host_bits <= 8) { // class C
			octets[3] = Integer.toString(Integer.parseInt(octets[3]) + incremental_value);
		} else if (host_bits > 8 && host_bits <= 16) { // class B
			octets[3] = Integer.toString(Integer.parseInt(octets[3]) + (incremental_value - 256));
			octets[2] = Integer.toString(Integer.parseInt(octets[2] + 1));
		} else if (host_bits > 16 && host_bits <= 24) { // class A
			octets[3] = Integer.toString(Integer.parseInt(octets[3]) + (incremental_value - 256));
			octets[2] = Integer.toString(Integer.parseInt(octets[2] + 1));
			octets[1] = Integer.toString(Integer.parseInt(octets[1] + 1));
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
		return octets[0] + "." + octets[1] + "." + octets[2] + ".0";
	}

	public String[] calculateNetworkRanges()
	{
		int nets = available_subnets > MainActivity.MAX_RANGES ? MainActivity.MAX_RANGES : available_subnets;
		String[] networks = new String[nets];
		String base, top;

		base = getBaseNetwork(network);
		networks[0] = base + " - " + getNextNetwork(base, hosts_per_subnet - 1);
		for (int k = 1; k < nets; k++) {
			base = getNextNetwork(base, hosts_per_subnet);
			top = getNextNetwork(base, hosts_per_subnet - 1);
			networks[k] = base + " - " + top;
		}
		return networks;
	}

    public String ipToHex(String ip)
    {
        String[] octets = ip.split("[.]");

        String hex = Integer.toHexString(Integer.parseInt(octets[0])) +
                Integer.toHexString(Integer.parseInt(octets[1])) +
                Integer.toHexString(Integer.parseInt(octets[2])) +
                Integer.toHexString(Integer.parseInt(octets[3]));

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

    public String splitIntoBinaryOctets(String b)
    {
        return Integer.toBinaryString(Integer.parseInt(b.substring(0, 8), 2)) + "."
                + Integer.toBinaryString(Integer.parseInt(b.substring(8, 16), 2)) + "."
                + Integer.toBinaryString(Integer.parseInt(b.substring(16, 24), 2)) + "."
                + Integer.toBinaryString(Integer.parseInt(b.substring(24, 32), 2));
    }

	public String octetToBinary(String octet)
	{
		return Integer.toBinaryString(Integer.parseInt(octet));
	}

	public String broadcast(String network, String netmask)
	{
		// bitwise OR the network IP and the inverted netmask (hostmask)
		return bitwiseOr(network, bitwiseInvert(netmask));
	}

	// The network plus one
    public String minimumHostAddress(String subnet_addr)
    {
        String[] octets = subnet_addr.split("[.]");

        octets[3] = Integer.toString(Integer.parseInt(octets[3]) + 1);
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

	// the broadcast minus one
    public String maximumHostAddress(String broadcast_ip)
    {
        String[] octets = broadcast_ip.split("[.]");

        octets[3] = Integer.toString(Integer.parseInt(octets[3]) - 1);
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

    public String bitwiseAnd(String ip1, String ip2) {
        String[] input1 = ip1.split("[.]");
        String[] input2 = ip2.split("[.]");
        String[] output = new String[4];

        for (int i = 0; i < 4; i++) {
            output[i] = Integer.toString(Integer.parseInt(input1[i]) & Integer.parseInt(input2[i]) );
        }
        return output[0] + "." + output[1] + "." + output[2] + "." + output[3];
    }

	public String bitwiseOr(String ip1, String ip2)
	{
		String[] input1 = ip1.split("[.]");
		String[] input2 = ip2.split("[.]");
		String[] output = new String[4];

		for (int i = 0; i < 4; i++) {
			output[i] = Integer.toString(Integer.parseInt(input1[i]) | Integer.parseInt(input2[i]) );
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

    public String getBinaryMaskOctets()
    {
        return splitIntoBinaryOctets(binary_mask);
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

    public int getUsableHosts()
    {
        return usable_hosts;
    }

    public int getNumberOfAddresses()
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

//	public int getNumberOfHosts()
//	{
//		return number_of_hosts;
//	}
//
//	public int getNumberOfNetworks()
//	{
//		return number_of_networks;
//	}
//
//	public String getOSM()
//	{
//		return osm;
//	}
//
//	public String getCsm()
//	{
//		return csm;
//	}

}
