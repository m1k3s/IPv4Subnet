package com.pbrane.mike.ipvxsubnet;


import java.util.regex.Pattern;

public class CalculateSubnetIPv6 {

	private static final Pattern IPV6_STD_PATTERN = Pattern.compile(""
			+ "^(((?=(?>.*?::)(?!.*::)))(::)?([0-9A-F]{1,4}::?){0,5}"
			+ "|([0-9A-F]{1,4}:){6})(\\2([0-9A-F]{1,4}(::?|$)){0,2}|((25[0-5]"
			+ "|(2[0-4]|1\\d|[1-9])?\\d)(\\.|$)){4}|[0-9A-F]{1,4}:[0-9A-F]{1,"
			+ "4})(?<![^:]:|\\.)\\z", Pattern.CASE_INSENSITIVE);
	private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile("((?:[0-9a-f]{1,4}(?::[0-9a-f]{1,4})*)?)::((?:[0-9a-f]{1,4}(?::[0-9a-f]{1,4})*)?)", Pattern.CASE_INSENSITIVE);
	private final int IPV6_ADDRESS_BITS = 128;
	String ipAddr;
	int network_bits;
	int host_bits;

	public void calculateSubnetIPv6(String ipAddr_mask)
	{
		if (ipAddr_mask.contains("/")) {
			String[] tmp = ipAddr_mask.split("[/]");
			ipAddr = tmp[0];
			network_bits = Integer.parseInt(tmp[1]);
			host_bits = IPV6_ADDRESS_BITS - network_bits;
		} else { // unicast address
			ipAddr = ipAddr_mask;
			network_bits = 64;
			host_bits = 64;
		}
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
