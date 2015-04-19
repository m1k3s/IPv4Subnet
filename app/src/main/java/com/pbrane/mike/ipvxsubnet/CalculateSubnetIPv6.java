/*
 * Copyright (c) 2015. Michael Sheppard
 */

package com.pbrane.mike.ipvxsubnet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

public class CalculateSubnetIPv6 {
	public final static CalculateSubnetIPv6 INSTANCE = new CalculateSubnetIPv6();
	private static final Pattern IPV6_STD_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
	private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");
	private final int IPV6_ADDRESS_BITS = 128;
	private String ModEUI64;
	private String linkLocal;
	private String ipAddr;
	private int network_bits;
	private int host_bits;

	private CalculateSubnetIPv6() {
		// avoid instantiation
	}

	public void CalculateSubnet(String cidrIP) {
		String[] tmp = cidrIP.split("[/]");
		ipAddr = tmp[0];
		network_bits = Integer.parseInt(tmp[1]);
		host_bits = IPV6_ADDRESS_BITS - network_bits;
	}

	public boolean validateIPv6Address(String addr) {
		if (validateStandardIPv6(addr)) {
			return true;
		} else if (validateCompressedIPv6(addr)) {
			return true;
		}
		return false;
	}

	public boolean validateStandardIPv6(String addr) {
		return IPV6_STD_PATTERN.matcher(addr).matches();
	}

	public boolean validateCompressedIPv6(String addr) {
		return IPV6_HEX_COMPRESSED_PATTERN.matcher(addr).matches();
	}

	public String MACToModifiedEUI64(String mac) {
		String[] bytes = mac.split("[:]"); // should be six words
		String modByte = bitwiseOrByte(bytes[0], "2");
		return (modByte + bytes[1] + ":" + bytes[2] + "ff:fe" + bytes[3] + ":" + bytes[4] + bytes[5]).toLowerCase();
	}

	public String linkLocalIPv6(String ModdedEUI64) {
		return "fe80::" + ModdedEUI64;
	}

	public String decompress(String addr) {
		StringBuilder ipv6 = new StringBuilder(addr);
		// Store the location where you need add zeroes that were removed during uncompression
		int tempCompressLocation = ipv6.indexOf("::");

		//if address was compressed and zeroes were removed, remove that marker i.e "::"
		if (tempCompressLocation != -1) {
			ipv6.replace(tempCompressLocation, tempCompressLocation + 2, ":");
		}

		//extract rest of the components by splitting them using ":"
		String[] addressComponents = ipv6.toString().split(":");

		for (int i = 0; i < addressComponents.length; i++) {
			StringBuilder uncompressedComponent = new StringBuilder("");
			for (int j = 0; j < 4 - addressComponents[i].length(); j++) {

				//add a padding of the ignored zeroes during compression if required
				uncompressedComponent.append("0");

			}
			uncompressedComponent.append(addressComponents[i]);

			//replace the compressed component with the uncompressed one
			addressComponents[i] = uncompressedComponent.toString();
		}

		//Iterate over the uncompressed address components to add the ignored "0000" components depending on position of "::"
		ArrayList<String> uncompressedAddressComponents = new ArrayList<String>();

		for (int i = 0; i < addressComponents.length; i++) {
			if (i == tempCompressLocation / 4) {
				for (int j = 0; j < 8 - addressComponents.length; j++) {
					uncompressedAddressComponents.add("0000");
				}
			}
			uncompressedAddressComponents.add(addressComponents[i]);
		}

		//iterate over the uncompressed components to append and produce a full address
		StringBuilder uncompressedAddress = new StringBuilder("");
		Iterator it = uncompressedAddressComponents.iterator();
		while (it.hasNext()) {
			uncompressedAddress.append(it.next().toString());
			uncompressedAddress.append(":");
		}
		uncompressedAddress.replace(uncompressedAddress.length() - 1, uncompressedAddress.length(), "");
		return uncompressedAddress.toString();
	}

	public String ipToBinary(String ip, boolean split) {
		String[] octets = ip.split("[:]");
		int count = octets.length;
		String binary = "";

		for (int k = 0; k < count; k++) {
			binary += byteToBinary(octets[k]);
			if (split && k != (count - 1)) {
				binary += ":";
			}
		}
		return binary;
	}

	// leading zero pad the word manually, toBinaryString doesn't
	public String byteToBinary(String octet) {
		String padding = "0000000000000000"; // 16 bits in a word
		String result = padding + Integer.toBinaryString(Integer.parseInt(octet, 16));
		return result.substring(result.length() - padding.length(), result.length());
	}

	// leading zero pad the hex chars manually, toHexString doesn't
	public String byteToHex(String octet) {
		String padding = "0000"; // 4 hex characters in a word
		String result = padding + Integer.toHexString(Integer.parseInt(octet));
		return result.substring(result.length() - padding.length(), result.length());
	}

	public String bitwiseAndByte(String byte1, String byte2) {
		return Integer.toString(Integer.parseInt(byte1) & Integer.parseInt(byte2));
	}

	public String bitwiseOrByte(String byte1, String byte2) {
		return Integer.toString(Integer.parseInt(byte1) | Integer.parseInt(byte2));
	}

	public String bitwiseInvertByte(String input) {
		return Integer.toString(255 - Integer.parseInt(input));
	}

	public String getModEUI64() {
		return ModEUI64;
	}

	public String getLinkLocal() {
		return linkLocal;
	}

}
