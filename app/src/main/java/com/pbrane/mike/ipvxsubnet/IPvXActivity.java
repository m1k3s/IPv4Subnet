package com.pbrane.mike.ipvxsubnet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Html;
import android.graphics.Typeface;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.HashMap;


public class IPvXActivity extends Activity implements KeyboardView.OnKeyboardActionListener, View.OnKeyListener {

	public static final int MAX_RANGES = 32; // maximum count of network ranges to display
    private CalculateSubnetIpv4 subnet = new CalculateSubnetIpv4();
    private TextView textView;
	private EditText editText;
	private enum AddrType { CIDR, IP_NETMASK, IP_ONLY, MULTICAST, RESERVED, INVALID }
	private AddrType addrType;

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		HashMap<String, String> keyCodeMap = new HashMap<>();
		keyCodeMap.put("1", "1");
		keyCodeMap.put("2", "2");
		keyCodeMap.put("3", "3");
		keyCodeMap.put("4", "4");
		keyCodeMap.put("5", "5");
		keyCodeMap.put("6", "6");
		keyCodeMap.put("7", "7");
		keyCodeMap.put("8", "8");
		keyCodeMap.put("9", "9");
		keyCodeMap.put("0", "0");
		keyCodeMap.put("-5", "DEL");
		keyCodeMap.put("47", "/");
		keyCodeMap.put("46", ".");
		keyCodeMap.put("58", ":");
		keyCodeMap.put("32", "SPACE");
		keyCodeMap.put("66", "CALC");

		String c = keyCodeMap.get(String.valueOf(keyCode));
		if (!(c == null)){
			editText.append(c);
		} else {
			switch(keyCode){
				case -5:
					if(editText.getText().toString().length() > 0) {
						editText.setText(editText.getText().toString().substring(0, editText.getText().toString().length() - 1));
					}
					break;
				case -4:
					processEntry();
					break;
			}
		}
		return false;
	}

	@Override
	public void onPress(int primaryCode) {

	}

	@Override
	public void onRelease(int primaryCode) {

	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {

	}

	@Override
	public void onText(CharSequence text) {

	}

	@Override
	public void swipeLeft() {

	}

	@Override
	public void swipeRight() {

	}

	@Override
	public void swipeDown() {

	}

	@Override
	public void swipeUp() {

	}

	private void toggleKeyboardVisibility() {
		KeyboardView keyboardView = (KeyboardView) findViewById(R.id.ipkeyboardview);
		int visibility = keyboardView.getVisibility();
		switch (visibility) {
			case View.VISIBLE:
				keyboardView.setVisibility(View.GONE);
				break;
			case View.GONE:
			case View.INVISIBLE:
				keyboardView.setVisibility(View.VISIBLE);
//				editText = ei;
				break;
		}
	}

	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ipvx);

		// Setup the textview widget
        textView = (TextView) findViewById(R.id.textView);
		textView.setTypeface(Typeface.MONOSPACE);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
		textView.setTextColor(Color.WHITE);

		KeyboardView keyview = (KeyboardView)findViewById(R.id.ipkeyboard);
		Keyboard keyboard = new Keyboard(this, R.xml.keyboard);
		keyview.setKeyboard(keyboard);
		keyview.setEnabled(true);
		keyview.setPreviewEnabled(true);
		keyview.setOnKeyListener(this);
		keyview.setOnKeyboardActionListener(this);

		editText = (EditText) findViewById(R.id.editText);
//		editText.setCursorVisible(false);
		editText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggleKeyboardVisibility();
			}
		});
		// validate IP address as it's entered
		editText.addTextChangedListener(new TextValidator(editText)
		{
			@Override
			public void validate(TextView textView, String text)
			{
				if (text.isEmpty()) { // nothing to validate
					addrType = AddrType.INVALID;
					return;
				}
				// We don't subnet Class D or E
				if (subnet.isClassD(text)) {
					textView.setTextColor(Color.RED);
					addrType = AddrType.MULTICAST;
					return;
				}
				if (subnet.isClassE(text)) {
					textView.setTextColor(Color.RED);
					addrType = AddrType.RESERVED;
					return;
				}
				// Class A, B, C networks are okay
				if (subnet.validateCIDR(text)) {
				   	textView.setTextColor(Color.GREEN);
				   	addrType = AddrType.CIDR;
			   	} else if (subnet.validateIPAndMaskOctets(text)) {
				   	textView.setTextColor(Color.GREEN);
				   	addrType = AddrType.IP_NETMASK;
			   	} else if (subnet.validateIPAddress(text)) {
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
		textView.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				switch (v.getId()) {
					case R.id.textView:
//						HideSoftKeyboard();
						toggleKeyboardVisibility();
						break;
				}
			}
		});
    }

	private abstract class TextValidator implements TextWatcher {
		private final TextView textView;

		public TextValidator(EditText editText)
		{
			this.textView = editText;
		}

		public abstract void validate(TextView textView, String text);

		@Override
		final public void afterTextChanged(Editable s)
		{
			String text = this.textView.getText().toString();
			validate(this.textView, text);
		}

		@Override
		final public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		final public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);

		String ipAddr = editText.getText().toString();
		savedInstanceState.putString("IPAddr", ipAddr);
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		String ipAddr = savedInstanceState.getString("IPAddr");
		editText.setText(ipAddr);
		validateAndCalculateSubnet(ipAddr);
	}

	// use the enter key to start the subnetting process
	@Override
	public boolean dispatchKeyEvent(@NonNull KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			processEntry();
			return true;
		}
		return super.dispatchKeyEvent(e);
	}

//	public void HideSoftKeyboard()
//	{
//		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//		imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
//	}

	public void processEntry()
	{
		final String IpAndMask = editText.getText().toString();
		saveIP(IpAndMask);
		validateAndCalculateSubnet(IpAndMask);
	}

	// save the IP/mask to the prefs file
	public void saveIP(String ip)
	{
		SharedPreferences sharedPrefs = this.getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(getString(R.string.savedIPv4), ip);
		editor.apply();
	}

	// The IP and netmask should already be validated. We are checking for
	// a valid type of address notation. You are allowed
	// to enter an IP only, this will assumed to be /32
	public void validateAndCalculateSubnet(final String ipAddr)
	{
		switch (addrType) {
			case CIDR:
				subnet.calculateSubnetCIDR(ipAddr);
				break;
			case IP_NETMASK:
				subnet.calculateSubnetCIDR(subnet.convertToCIDR(ipAddr));
				break;
			case IP_ONLY:
				subnet.calculateSubnetCIDR(ipAddr + "/32"); // assume /32
				break;
			case MULTICAST:
				displayMulticastError();
				return;
			case RESERVED:
				displayReservedError();
				return;
			case INVALID:
			default:
				displayError();
				return;
		}
		displayResults();
	}

	// 'Clr' button callback
    public void on_clr(View view)
    {
		editText.setText("");
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

	protected void displayLogo()
	{
		String logoString = "<small><font color=#4169E1><b>IPv4</font>"
				+ "<font color=#00CC00>Subnet\u00A0-\u00A0</b></font>"
				+ "<font color=#C5C5C5><u><b>Michael</b></u></font>"
				+ "<font color=#DF0000><u>Sheppard</u></font>"
				+ "<font color=#4169E1>\u00A0-\u00A0<b>2015</b></font>\n";

		textView.append("\n");
		textView.append("--------------------------\n");

		textView.append(Html.fromHtml(logoString));
	}

	public void displayMulticastError()
	{
		String str = "<font color=#FF0000><b>ERROR: Subnetting Class D (Multicast) networks is not supported!<br>"
				+ "A Class D (Multicast) network is in the range 224.0 0 0 to 239.255.255.255"
				+ "This address range is used for host groups or multicast groups such as in EIGRP</b></font>\n";
		textView.setText("");
		textView.append(Html.fromHtml(str));
	}

	public void displayReservedError()
	{
		String str = "<font color=#FF0000><b>ERROR: Subnetting Class E (Reserved) networks is not supported!<br>"
				+ "A Class E (Reserved) network is in the range 240.0.0.0 255.255.255.255<br>"
				+ "This network is reserved by IANA for future use.</b></font>\n";
		textView.setText("");
		textView.append(Html.fromHtml(str));
	}

	public void displayError()
	{
		String str = "<font color=#FF0000><b>ERROR: Invalid IP Address or Mask! (How did you do that?)</b></font>\n";
		textView.setText("");
		textView.append(Html.fromHtml(str));
	}

	public String formatNumber(long number)
	{
		String result = "";
		if (number < 10000000) {
			result = String.format("%d", number);
		} else if (number >= 10000000 && number < 100000000) {
			result = String.format("%.4fM", number / 1000000.0);
		} else if (number >= 100000000) {
			result = String.format("%.4fG", number / 1000000000.0);
		}
		return result;
	}

	//
	// Private IP ranges:
	// Class A: 10.0.0.0 - 10.255.255.255
	// Class B: 172.16.0.0 - 172.31.255.255
	// Class C: 192.168.0.0 - 192.168.255.255
	//
	public String getPrivateIpRangesString(String ip)
	{
		long ipDec = Long.parseLong(subnet.ipToDecimal(ip));
		long ipLow, ipHigh;
		String comment = "";

		if (subnet.isClassA(ip)) {
			ipLow = Long.parseLong(subnet.ipToDecimal("10.0.0.0"));
			ipHigh = Long.parseLong(subnet.ipToDecimal("10.255.255.255"));
			if (ipDec >= ipLow && ipDec <= ipHigh) {
				comment = "Range: 10.0.0.0 to 10.255.255.255";
			}
		} else if (subnet.isClassB(ip)) {
			ipLow = Long.parseLong(subnet.ipToDecimal("172.16.0.0"));
			ipHigh = Long.parseLong(subnet.ipToDecimal("172.31.255.255"));
			if (ipDec >= ipLow && ipDec <= ipHigh) {
				comment = "Range: 172.16.0.0 to 172.31.255.255";
			}
		} else if (subnet.isClassC(ip)) {
			ipLow = Long.parseLong(subnet.ipToDecimal("192.168.0.0"));
			ipHigh = Long.parseLong(subnet.ipToDecimal("192.168.255.255"));
			if (ipDec >= ipLow && ipDec <= ipHigh) {
				comment = "Range: 192.168.0.0 to 192.168.255.255";
			}
		}
		return comment;
	}

    public void displayResults()
    {
        textView.setText(""); // clear TextView

		// [Classful]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Classful]</b></font><br>"));
		String hostIP = subnet.getIpAddr();
		textView.append(String.format("%-25s%s\n", "Host Address:", hostIP));
		textView.append(String.format("%-25s%s\n", "Host Address (decimal):", subnet.getIpAddrDecimal()));
		textView.append(String.format("%-25s%s\n", "Host Address (hex):", subnet.getIpAddrHex()));
		textView.append(String.format("%-25s%s\n", "Network Class:", subnet.getNetworkClass(hostIP)));
		String baseNetwork = subnet.getBaseNetwork(hostIP);
		String baseNetworkMask = subnet.getNetworkClassMask(baseNetwork);
		textView.append(String.format("%-25s%s\n", "Network Address:", baseNetwork));
		textView.append(String.format("%-25s%s\n", "Network Mask:", baseNetworkMask));
		textView.append(String.format("%-25s%s\n", "Network Mask (hex):", subnet.ipToHex(baseNetworkMask)));
		textView.append(String.format("%-25s%s\n", "Broadcast:", subnet.getNetworkClassBroadcast(baseNetwork)));
		String nHosts = NumberFormat.getNumberInstance().format(subnet.calcHostsForNetworkClass(baseNetwork) - 2);
		textView.append(String.format("%-25s%s\n", "Number of hosts:", nHosts));
		textView.append("\n");

		// [CIDR]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[CIDR]</b></font><br>"));
		textView.append(String.format("%-25s%s\n", "Host Address:", hostIP));
		textView.append(String.format("%-25s%s\n", "Host Address (decimal):", subnet.getIpAddrDecimal()));
		textView.append(String.format("%-25s%s\n", "Host Address (hex):", subnet.getIpAddrHex()));
		textView.append(String.format("%-25s%s\n", "Network Address:", subnet.getNetwork()));
		String mask = subnet.getDecimalMaskOctets();
		textView.append(String.format("%-25s%s\n", "NetMask:", mask));
		String nhbits = Integer.toString(subnet.getNetworkBits()) + " / " +Integer.toString(subnet.getHostBits());
		textView.append(String.format("%-25s%s\n", "Net/Host Mask (bits):", nhbits));
		textView.append(String.format("%-25s%s\n", "NetMask (hex):", subnet.ipToHex(mask)));
		textView.append(String.format("%-25s%s\n", "Broadcast:", subnet.getBroadcast()));
		textView.append(String.format("%-25s%s\n", "Cisco Wildcard:", subnet.getWildcard()));
		long usable = subnet.getUsableHosts();
		long total = subnet.getNumberOfAddresses();
		String utips = formatNumber(usable) + " / " + formatNumber(total);
		textView.append(String.format("%-25s%s\n", "Usable/Total IPs:", utips));
		textView.append(String.format("%-9s%-15s - %-15s\n", "Network:", subnet.getNetwork(),subnet.getBroadcast()));
		if (usable == 0) {
			textView.append(Html.fromHtml("<font color=#FF0000><b>Usable:\u00A0\u00A00</b></font><br\n"));
		} else if (usable == 1) {
			textView.append(String.format("%-9s%-15s\n", "Usable:", 1));
		} else {
			textView.append(String.format("%-9s%-15s - %-15s\n", "Usable:", subnet.getMinHostAddr(), subnet.getMaxHostAddr()));
		}
		String nSubnets = NumberFormat.getNumberInstance().format(subnet.getAvailableSubnets());
		textView.append(String.format("%-25s%s\n", "Available Subnets:", nSubnets));
		textView.append("\n");

		// [Classfull Bitmaps]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Classful Bitmaps]</b></font><br>"));
		textView.append(String.format("%-10s%s\n", "Host IP:", subnet.ipToBinary(hostIP, true)));
		textView.append(String.format("%-10s%s\n", "Net IP:", subnet.ipToBinary(baseNetwork, true)));
		textView.append(String.format("%-10s%s\n", "Netmask:", subnet.ipToBinary(baseNetworkMask, true)));
		textView.append(String.format("%-10s%s\n", "Brdcast:", subnet.ipToBinary(subnet.getNetworkClassBroadcast(baseNetwork), true)));
		textView.append("\n");

		// [CIDR Bitmaps]
		textView.append(Html.fromHtml("<font color=#00BFFF><b>[CIDR Bitmaps]</b></font><br>"));
		textView.append(String.format("%-10s%s\n", "Host IP:", subnet.ipToBinary(hostIP, true)));
		textView.append(String.format("%-10s%s\n", "Net IP:", subnet.ipToBinary(baseNetwork, true)));
		textView.append(String.format("%-10s%s\n", "Netmask:", subnet.ipToBinary(mask, true)));
		textView.append(String.format("%-10s%s\n", "Brdcast:", subnet.ipToBinary(subnet.getBroadcast(), true)));
		textView.append(String.format("%-10s%s\n", "Wildcard:", subnet.ipToBinary(subnet.getWildcard(), true)));
		textView.append("\n");

		// [Networks]

		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Networks]</b></font><br>"));
		if (usable == 1) { // only one host
			textView.append(String.format("%3d. %-15s -\n", 1, hostIP));
		} else if (usable > 1) { // multiple subnets
			String[] ranges = subnet.getRanges();
			int count = 1;
			for (String range : ranges) {
				String low = range.split(" - ")[0];
				String high = range.split(" - ")[1];
				if (low.equals(subnet.getNetwork())) {
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
		if (subnet.isLoopBackOrDiagIP(hostIP)) {
			textView.append("\n");
			String loopIPComment = "<font color=#FFD700> * This host IP address is in the range of IPs"
					+ " used for loopback and diagnostic purposes.</font><br>";
			textView.append(Html.fromHtml(loopIPComment));
		}
		if (subnet.isPrivateIP(hostIP)) {
			textView.append("\n");
			String privateIPComment = "<font color=#FFD700> * This host IP address is in a private"
			+ " IP range and cannot be routed on the public network. Routers on the Internet should"
			+ " be configured to discard these IPs.<br>" + getPrivateIpRangesString(hostIP) + "</font><br>";
			textView.append(Html.fromHtml(privateIPComment));
		}
		displayLogo();
		toggleKeyboardVisibility();
//		HideSoftKeyboard();
    }
}
