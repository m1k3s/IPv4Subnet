package com.pbrane.mike.ipv4subnet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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


public class MainActivity extends Activity {

	public static final int MAX_RANGES = 32; // maximum count of network ranges to display
    private CalculateSubnet subnet = new CalculateSubnet();
    private TextView textView;
	private EditText editText;
	private enum AddrType { CIDR, IP_NETMASK, IP_ONLY, MULTICAST, RESERVED, INVALID }
	private AddrType addrType;

	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
		// Setup the textview widget
        textView = (TextView) findViewById(R.id.textView);
		textView.setTypeface(Typeface.MONOSPACE);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
		textView.setTextColor(Color.WHITE);

		editText = (EditText) findViewById(R.id.editText);
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
			String ipAddr = sharedPref.getString(getString(R.string.savedIP), "");
			editText.setText(ipAddr);
		}

		// hide the softkeyboard when the textview is clicked
		textView.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				switch (v.getId()) {
					case R.id.textView:
						HideSoftKeyboard();
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

	// use the enter key to start the process
	@Override
	public boolean dispatchKeyEvent(@NonNull KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			processEntry();
			return true;
		}
		return super.dispatchKeyEvent(e);
	}

	public void HideSoftKeyboard()
	{
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
//		imm.toggleSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
	}

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
		editor.putString(getString(R.string.savedIP), ip);
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

    public void displayResults()
    {
        textView.setText(""); // clear TextView

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
		String utips = Integer.toString(subnet.getUsableHosts()) + " / " + Integer.toString(subnet.getNumberOfAddresses());
		textView.append(String.format("%-25s%s\n", "Usable/Total IPs:", utips));
		textView.append(String.format("%-9s%-15s - %-15s\n", "Network:", subnet.getNetwork(),subnet.getBroadcast()));
		textView.append(String.format("%-9s%-15s - %-15s\n", "Usable:", subnet.getMinHostAddr(), subnet.getMaxHostAddr()));
		String nSubnets = NumberFormat.getNumberInstance().format(subnet.getAvailableSubnets());
		textView.append(String.format("%-25s%s\n", "Available Subnets:", nSubnets));
		textView.append("\n");

		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Classful Bitmaps]</b></font><br>"));
		textView.append(String.format("%-10s%s\n", "Host IP:", subnet.ipToBinary(hostIP, true)));
		textView.append(String.format("%-10s%s\n", "Net IP:", subnet.ipToBinary(baseNetwork, true)));
		textView.append(String.format("%-10s%s\n", "Netmask:", subnet.ipToBinary(baseNetworkMask, true)));
		textView.append(String.format("%-10s%s\n", "Brdcast:", subnet.ipToBinary(subnet.getNetworkClassBroadcast(baseNetwork), true)));
		textView.append("\n");

		textView.append(Html.fromHtml("<font color=#00BFFF><b>[CIDR Bitmaps]</b></font><br>"));
		textView.append(String.format("%-10s%s\n", "Host IP:", subnet.ipToBinary(hostIP, true)));
		textView.append(String.format("%-10s%s\n", "Net IP:", subnet.ipToBinary(baseNetwork, true)));
		textView.append(String.format("%-10s%s\n", "Netmask:", subnet.ipToBinary(mask, true)));
		textView.append(String.format("%-10s%s\n", "Brdcast:", subnet.ipToBinary(subnet.getBroadcast(), true)));
		textView.append(String.format("%-10s%s\n", "Wildcard:", subnet.ipToBinary(subnet.getWildcard(), true)));
		textView.append("\n");

		textView.append(Html.fromHtml("<font color=#00BFFF><b>[Networks]</b></font><br>"));
		String[] ranges = subnet.getRanges();
		int count = 1;
		for (String range : ranges) {
			String low = range.split(" - ")[0];
			String high = range.split(" - ")[1];
			if (low.equals(subnet.getNetwork())) {
				textView.append(String.format("%3d. %-15s - %-15s", count, low, high));
				textView.append(" <==\n");
			} else {
				textView.append(String.format("%3d. %-15s - %-15s\n", count, low, high));
			}
			count++;
		}
		// check for loopback or diag IP first, then private IP
		if (subnet.isLoopBackOrDiagIP(hostIP)) {
			textView.append("\n");
			textView.append(Html.fromHtml("<font color=#00BFFF><b>[Note]</b></font><br>"));
			String loopIPComment = "<font color=#FFFFE0>This host IP address is in the range of IPs"
					+ " used for loopback and diagnostic purposes.</font><br>";
			textView.append(Html.fromHtml(loopIPComment));
		} else if (subnet.isPrivateIP(hostIP)) {
			textView.append("\n");
			textView.append(Html.fromHtml("<font color=#00BFFF><b>[Note]</b></font><br>"));
			String privateIPComment = "<font color=#FFFFE0>This host IP address is in a private"
			+ " IP range and cannot be routed on the public network. Routers on the Internet should"
			+ " be configured to discard these IPs.</font><br>";
			textView.append(Html.fromHtml(privateIPComment));
		}
		displayLogo();
		HideSoftKeyboard();
    }
}
