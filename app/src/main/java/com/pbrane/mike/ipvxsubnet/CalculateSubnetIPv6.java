package com.pbrane.mike.ipvxsubnet;


import java.util.regex.Pattern;

public class CalculateSubnetIPv6 {

	private static final Pattern IPV6_STD_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
	private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");
	private final int IPV6_ADDRESS_BITS = 128;
	String ipAddr;
	int network_bits;
	int host_bits;

	public void calculateSubnetIPv6(String ipAddr_mask)
	{
		String[] tmp = ipAddr_mask.split("[/]");
		ipAddr = tmp[0];
		network_bits = Integer.parseInt(tmp[1]);
		host_bits = IPV6_ADDRESS_BITS - network_bits;
	}

	public boolean validateIPv6Address(String addr)
	{
		if (validateStandardIPv6(addr)) {
			return true;
		} else if (validateCompressedIPv6(addr)) {
			return true;
		}
		return false;
	}

	public boolean validateStandardIPv6(String addr)
	{
		return IPV6_STD_PATTERN.matcher(addr).matches();
	}

	public boolean validateCompressedIPv6(String addr)
	{
		return IPV6_HEX_COMPRESSED_PATTERN.matcher(addr).matches();
	}
}
