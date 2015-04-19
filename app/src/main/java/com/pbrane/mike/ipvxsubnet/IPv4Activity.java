package com.pbrane.mike.ipvxsubnet;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Html;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.text.NumberFormat;


public class IPv4Activity extends Activity {

	private String version;
	public static final int MAX_RANGES = 32; // maximum count of network ranges to display
	private CalculateSubnetIPv4 subnet4 = CalculateSubnetIPv4.INSTANCE;
	private CalculateSubnetIPv6 subnet6 = CalculateSubnetIPv6.INSTANCE;
	private TextView textView;
	private EditText editText;
	private CustomIPv4Keyboard customIPv4Keyboard = CustomIPv4Keyboard.INSTANCE;

	private enum AddrType {CIDR, IP_NETMASK, IP_ONLY, MULTICAST, RESERVED, INVALID}

	private AddrType addrType = AddrType.INVALID; // initialize to invalid

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_ipvx);

		version = getVersionString();

		// Setup the textview widget
		textView = (TextView) findViewById(R.id.textView);
		textView.setTypeface(Typeface.MONOSPACE);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
		textView.setTextColor(Color.WHITE);

		editText = (EditText) findViewById(R.id.editText);

		// initialize the instance variable customIPv4Keyboard
		customIPv4Keyboard.init(this, R.id.keyboardview, R.xml.keyboard);
		// register the edittext
		customIPv4Keyboard.registerEditText(R.id.editText);

		// IPv6 TEST ***
		Log.i("ModdedEUI64", subnet6.MACToModifiedEUI64("00:1c:bf:62:e7:13"));
		Log.i("LinkLocal IP", subnet6.linkLocalIPv6(subnet6.MACToModifiedEUI64("00:1c:bf:62:e7:13")));
		Log.i("Decompressed IP", subnet6.decompress(subnet6.linkLocalIPv6(subnet6.MACToModifiedEUI64("00:1c:bf:62:e7:13"))));

		editText.setKeyListener(new DialerKeyListener() {
			@Override
			public int getInputType() {
				// should be the same as android:inputType:
				return (InputType.TYPE_CLASS_PHONE);
			}

			@Override
			protected char[] getAcceptedChars() {
				return new char[]{
						'1', '2', '3', '/',
						'4', '5', '6', '.',
						'7', '8', '9', //[backspace]
						     '0', ' ', //[enter]
				};
			}
		});
		// validate IP address as it's entered
		editText.addTextChangedListener(new TextValidator(editText) {
			@Override
			public void validate(TextView textView, String text) {
				// crap check - not valid if empty or doesn't start with a digit
				if (text.isEmpty() || !Character.isDigit(text.charAt(0))) {
					textView.setTextColor(Color.RED);
					addrType = AddrType.INVALID;
					return;
				}
				// We don't subnet Class D or E
				if (subnet4.isClassD(text)) {
					textView.setTextColor(Color.RED);
					addrType = AddrType.MULTICAST;
					return;
				}
				if (subnet4.isClassE(text)) {
					textView.setTextColor(Color.RED);
					addrType = AddrType.RESERVED;
					return;
				}
				// Class A, B, C networks are okay
				if (subnet4.validateCIDR(text)) {
					textView.setTextColor(Color.GREEN);
					addrType = AddrType.CIDR;
				} else if (subnet4.validateIPAndMaskOctets(text)) {
					textView.setTextColor(Color.GREEN);
					addrType = AddrType.IP_NETMASK;
				} else if (subnet4.validateIPAddress(text)) {
					textView.setTextColor(Color.GREEN);
					addrType = AddrType.IP_ONLY;
				} else {
					textView.setTextColor(Color.RED);
					addrType = AddrType.INVALID;
				}
			}
		});

		if (savedInstanceState != null) { // restore saved state
			String ipAddr = savedInstanceState.getString("IPAddr");
			editText.setText(ipAddr);
			validateAndCalculateSubnet(ipAddr);
		} else { // get the last IP/mask used and insert in editText
			SharedPreferences sharedPref = this.getPreferences(MODE_PRIVATE);
			String ipAddr = sharedPref.getString(getString(R.string.savedIPv4), "");
			editText.setText(ipAddr);
		}

		// hide the softkeyboard when the textview is clicked
		textView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				switch (v.getId()) {
					case R.id.textView:
						customIPv4Keyboard.hideCustomKeyboard();
						break;
				}
			}
		});
	}

	public String getVersionString() {
		PackageInfo pacInfo;
		String version;
		String build = "";
		try {
			pacInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
			String[] tmp = pacInfo.versionName.split("-");
			version = tmp[0];
			build = tmp.length > 1 ? tmp[1] : "";
		} catch (PackageManager.NameNotFoundException e) {
			Log.e("NameNotFoundException", e.toString());
			version = "kr4p";
		}
		String padding = "0000"; // 4 digits in build number
		String result = padding + build;
		return version + "." + result.substring(result.length() - padding.length(), result.length());
	}

	private abstract class TextValidator implements TextWatcher {
		private final TextView validatorTextView;

		public TextValidator(EditText editText) {
			this.validatorTextView = editText;
		}

		public abstract void validate(TextView textView, String text);

		@Override
		final public void afterTextChanged(Editable s) {
			String text = this.validatorTextView.getText().toString();
			validate(this.validatorTextView, text);
		}

		@Override
		final public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		final public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
	}

	public TextView getTextView() {
		return textView;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		String ipAddr = editText.getText().toString();
		savedInstanceState.putString("IPAddr", ipAddr);
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		String ipAddr = savedInstanceState.getString("IPAddr");
		editText.setText(ipAddr);
		validateAndCalculateSubnet(ipAddr);
	}

	// use the keyboard enter key to start the subnetting process
	@Override
	public boolean dispatchKeyEvent(@NonNull KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			if (e.getAction() == KeyEvent.ACTION_UP) {
				processEntry();
				return true;
			}
		}
		return super.dispatchKeyEvent(e);
	}

	public void processEntry() {
		final String IpAndMask = editText.getText().toString();
		saveIP(IpAndMask);
		validateAndCalculateSubnet(IpAndMask);
	}

	// save the IP/mask to the prefs file
	public void saveIP(String ip) {
		SharedPreferences sharedPrefs = this.getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(getString(R.string.savedIPv4), ip);
		editor.apply();
	}

	// The IP and netmask should already be validated. We are checking for
	// a valid type of address notation. You are allowed
	// to enter an IP only, this will assumed to be /32
	public void validateAndCalculateSubnet(final String ipAddr) {
		boolean result;
		switch (addrType) {
			case CIDR:
				result = subnet4.calculateSubnetCIDR(ipAddr);
				break;
			case IP_NETMASK:
				result = subnet4.calculateSubnetCIDR(subnet4.convertToCIDR(ipAddr));
				break;
			case IP_ONLY:
				result = subnet4.calculateSubnetCIDR(ipAddr + "/32"); // assume /32
				break;
			case MULTICAST:
				displayMulticastInfoMessage();
				return;
			case RESERVED:
				displayReservedRangeInfoMessage();
				return;
			case INVALID:
			default:
				displayError();
				return;
		}
		if (result) {
			displayResults();
		} else {
			displayErrorMessage("Something has gone horribly wrong! I'm pretty sure it was YOUR fault.");
		}
	}

	protected void displayVersionLogo() {
		String logoString = "<small><font color=#4169E1><b>IPvX</font>"
				+ "<font color=#00CC00>Subnet\u00A0-\u00A0</b></font>"
				+ "<font color=#C5C5C5><u><b>Michael</b></u></font>"
				+ "<font color=#DF0000><u>Sheppard</u></font>"
				+ "<font color=#4169E1>\u00A0-\u00A0<b>&copy 2015</b></font>"
				+ "<font color=#C5C5C5>\u00A0v" + version + "</font>\n";

		textView.append("\n\n");
		textView.append(Html.fromHtml(logoString));
	}

	public void displayMulticastInfoMessage() {
		String str = "<font color=#FF0000><b>INFO:</font><font color=#FFD700> Subnetting Class D (Multicast)"
				+ " networks is not supported!<br><br>A Class D (Multicast) network is in the range 224.0 0 0 to 239.255.255.255."
				+ " This address range is used for host groups or multicast groups such as in EIGRP</b></font>\n";
		textView.setText("");
		textView.append(Html.fromHtml(str));
	}

	public void displayReservedRangeInfoMessage() {
		String str = "<font color=#FF0000><b>INFO:</font><font color=#FFD700> Subnetting Class E (Reserved)"
				+ " networks is not supported!<br><br> A Class E (Reserved) network is in the range 240.0.0.0 255.255.255.255."
				+ " This address range is reserved by IANA for future use.</b></font>\n";
		textView.setText("");
		textView.append(Html.fromHtml(str));
	}

	public void displayError() {
		String str = "<font color=#FF0000><b>ERROR:</font><font color=#FFD700> Invalid IP Address or Mask! (How did you do that?)</b></font>\n";
		textView.setText("");
		textView.append(Html.fromHtml(str));
	}

	public void displayErrorMessage(String errorMsg) {
		String str = "<font color=#FF0000><b>ERROR:</font><font color=#FFD700> " + errorMsg + "</b></font>\n";
		textView.setText("");
		textView.append(Html.fromHtml(str));
	}

	public String formatNumber(long number) {
		String result = "";
		if (number < 1e6) { // 0 to 999.999K
			result = String.format("%d", number);
		} else if (number >= 1e6 && number < 1e8) { // 1M to 99.999999M
			result = String.format("%.4fM", number / 1e6);
		} else if (number >= 1e8) { // 100M and above
			result = String.format("%.4fG", number / 1e9);
		}
		return result;
	}

	//
	// Private IP ranges:
	// Class A: 10.0.0.0 - 10.255.255.255
	// Class B: 172.16.0.0 - 172.31.255.255
	// Class C: 192.168.0.0 - 192.168.255.255
	//
	public String getPrivateIpRangesString(String ip) {
		long ipDec = Long.parseLong(subnet4.ipToDecimal(ip));
		long ipLow, ipHigh;
		String comment = "";

		if (subnet4.isClassA(ip)) {
			ipLow = Long.parseLong(subnet4.ipToDecimal("10.0.0.0"));
			ipHigh = Long.parseLong(subnet4.ipToDecimal("10.255.255.255"));
			if (ipDec >= ipLow && ipDec <= ipHigh) {
				comment = "Range: 10.0.0.0 to 10.255.255.255";
			}
		} else if (subnet4.isClassB(ip)) {
			ipLow = Long.parseLong(subnet4.ipToDecimal("172.16.0.0"));
			ipHigh = Long.parseLong(subnet4.ipToDecimal("172.31.255.255"));
			if (ipDec >= ipLow && ipDec <= ipHigh) {
				comment = "Range: 172.16.0.0 to 172.31.255.255";
			}
		} else if (subnet4.isClassC(ip)) {
			ipLow = Long.parseLong(subnet4.ipToDecimal("192.168.0.0"));
			ipHigh = Long.parseLong(subnet4.ipToDecimal("192.168.255.255"));
			if (ipDec >= ipLow && ipDec <= ipHigh) {
				comment = "Range: 192.168.0.0 to 192.168.255.255";
			}
		}
		return comment;
	}

	public void displayResults() {
		textView.setText(""); // clear TextView

		// [Classful]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Classful]</b></font><br>"));
		String hostIP = subnet4.getIpAddr();
		textView.append(String.format("%-25s%s\n", "Host Address:", hostIP));
		textView.append(String.format("%-25s%s\n", "Host Address (decimal):", subnet4.getIpAddrDecimal()));
		textView.append(String.format("%-25s%s\n", "Host Address (hex):", subnet4.getIpAddrHex()));
		textView.append(String.format("%-25s%s\n", "Network Class:", subnet4.getNetworkClass(hostIP)));
		String baseNetwork = subnet4.getBaseNetwork(hostIP);
		String baseNetworkMask = subnet4.getNetworkClassMask(baseNetwork);
		textView.append(String.format("%-25s%s\n", "Network Address:", baseNetwork));
		textView.append(String.format("%-25s%s\n", "Network Mask:", baseNetworkMask));
		textView.append(String.format("%-25s%s\n", "Network Mask (hex):", subnet4.ipToHex(baseNetworkMask)));
		textView.append(String.format("%-25s%s\n", "Broadcast:", subnet4.getNetworkClassBroadcast(baseNetwork)));
		String nHosts = NumberFormat.getNumberInstance().format(subnet4.calcHostsForNetworkClass(baseNetwork) - 2);
		textView.append(String.format("%-25s%s\n", "Number of hosts:", nHosts));
		textView.append("\n");

		// [CIDR]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[CIDR]</b></font><br>"));
		textView.append(String.format("%-25s%s\n", "Host Address:", hostIP));
		textView.append(String.format("%-25s%s\n", "Host Address (decimal):", subnet4.getIpAddrDecimal()));
		textView.append(String.format("%-25s%s\n", "Host Address (hex):", subnet4.getIpAddrHex()));
		textView.append(String.format("%-25s%s\n", "Network Address:", subnet4.getNetwork()));
		String mask = subnet4.getDecimalMaskOctets();
		textView.append(String.format("%-25s%s\n", "NetMask:", mask));
		String nhbits = Integer.toString(subnet4.getNetworkBits()) + " / " + Integer.toString(subnet4.getHostBits());
		textView.append(String.format("%-25s%s\n", "Net/Host Mask (bits):", nhbits));
		textView.append(String.format("%-25s%s\n", "NetMask (hex):", subnet4.ipToHex(mask)));
		textView.append(String.format("%-25s%s\n", "Broadcast:", subnet4.getBroadcast()));
		textView.append(String.format("%-25s%s\n", "Cisco Wildcard:", subnet4.getWildcard()));
		long usable = subnet4.getUsableHosts();
		long total = subnet4.getNumberOfAddresses();
		String utips = formatNumber(usable) + " / " + formatNumber(total);
		textView.append(String.format("%-25s%s\n", "Usable/Total IPs:", utips));
		textView.append(String.format("%-9s%-15s - %-15s\n", "Network:", subnet4.getNetwork(), subnet4.getBroadcast()));
		if (usable == 0) {
			textView.append(Html.fromHtml("<font color=#FF0000><b>Usable:\u00A0\u00A00</b></font><br\n"));
		} else if (usable == 1) {
			textView.append(String.format("%-9s%-15s\n", "Usable:", 1));
		} else {
			textView.append(String.format("%-9s%-15s - %-15s\n", "Usable:", subnet4.getMinHostAddr(), subnet4.getMaxHostAddr()));
		}
		String nSubnets = NumberFormat.getNumberInstance().format(subnet4.getAvailableSubnets());
		textView.append(String.format("%-25s%s\n", "Available Subnets:", nSubnets));
		textView.append("\n");

		// [Classfull Bitmaps]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Classful Bitmaps]</b></font><br>"));
		textView.append(String.format("%-10s%s\n", "Host IP:", subnet4.ipToBinary(hostIP, true)));
		textView.append(String.format("%-10s%s\n", "Net IP:", subnet4.ipToBinary(baseNetwork, true)));
		textView.append(String.format("%-10s%s\n", "Netmask:", subnet4.ipToBinary(baseNetworkMask, true)));
		textView.append(String.format("%-10s%s\n", "Brdcast:", subnet4.ipToBinary(subnet4.getNetworkClassBroadcast(baseNetwork), true)));
		textView.append("\n");

		// [CIDR Bitmaps]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[CIDR Bitmaps]</b></font><br>"));
		textView.append(String.format("%-10s%s\n", "Host IP:", subnet4.ipToBinary(hostIP, true)));
		textView.append(String.format("%-10s%s\n", "Net IP:", subnet4.ipToBinary(baseNetwork, true)));
		textView.append(String.format("%-10s%s\n", "Netmask:", subnet4.ipToBinary(mask, true)));
		textView.append(String.format("%-10s%s\n", "Brdcast:", subnet4.ipToBinary(subnet4.getBroadcast(), true)));
		textView.append(String.format("%-10s%s\n", "Wildcard:", subnet4.ipToBinary(subnet4.getWildcard(), true)));
		textView.append("\n");

		// [Networks]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Networks]</b></font><br>"));
		if (usable == 1) { // only one host
			textView.append(String.format("%3d. %-15s -\n", 1, hostIP));
		} else if (usable > 1) { // one or more subnets
			String[] ranges = subnet4.getRanges();
			int count = 1;
			for (String range : ranges) {
				String low = range.split(" - ")[0];
				String high = range.split(" - ")[1];
				if (low.equals(subnet4.getNetwork())) {
					textView.append(String.format("%3d. %-15s - %-15s", count, low, high));
					textView.append(Html.fromHtml("<font color=#00ff00><b>\u00A0&lt==</b></font><br>\n"));
				} else {
					textView.append(String.format("%3d. %-15s - %-15s\n", count, low, high));
				}
				count++;
			}
		} else { // all addresses are unusable, e.g. /31
			textView.append("\n");
		}

		// [Notes]
		textView.append("\n");
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Notes]</b></font><br>"));
		// check for unusable address, loopback or diag IP, then private IP
		if (usable == 0) {
			textView.append("\n");
			String noUsableIPsComment = "<font color=#FFD700> * There are no usable hosts in this subnet.</font><br>";
			textView.append(Html.fromHtml(noUsableIPsComment));
		}
		if (subnet4.isLoopBackOrDiagIP(hostIP)) {
			textView.append("\n");
			String loopIPComment = "<font color=#FFD700> * This host IP address is in the range of IPs"
					+ " used for loopback and diagnostic purposes.</font><br>";
			textView.append(Html.fromHtml(loopIPComment));
		}
		if (subnet4.isPrivateIP(hostIP)) {
			textView.append("\n");
			String privateIPComment = "<font color=#FFD700> * This host IP address is in a private"
					+ " IP range and cannot be routed on the public network. Routers on the Internet should"
					+ " be configured to discard these IPs.<br>" + getPrivateIpRangesString(hostIP) + "</font><br>";
			textView.append(Html.fromHtml(privateIPComment));
		}
		displayVersionLogo();
		customIPv4Keyboard.hideCustomKeyboard();
	}
}
