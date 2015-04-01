package com.pbrane.mike.ipv4subnet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity {

	private static final int MAX_HOSTS_TO_DISPLAY = 16;
    private CalculateSubnet subnet = new CalculateSubnet();
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);

		if (savedInstanceState != null) { // restore saved state
			String ipAddr = savedInstanceState.getString("IPAddr");
			EditText editText = (EditText) findViewById(R.id.editText);
			editText.setText(ipAddr);
			validateAndCalculateSubnet(ipAddr);
		} else { // get the last IP/mask used and insert in editText
			SharedPreferences sharedPref = this.getPreferences(MODE_PRIVATE);
			String ipAddr = sharedPref.getString(getString(R.string.savedIP), "");
			EditText editText = (EditText) findViewById(R.id.editText);
			editText.setText(ipAddr);
		}
    }

	@Override
	public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);

		EditText editText = (EditText) findViewById(R.id.editText);
		String ipAddr = editText.getText().toString();
		savedInstanceState.putString("IPAddr", ipAddr);
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		String ipAddr = savedInstanceState.getString("IPAddr");
		EditText editText = (EditText) findViewById(R.id.editText);
		editText.setText(ipAddr);
		validateAndCalculateSubnet(ipAddr);
	}

	// use the enter key to start the process
	@Override
	public boolean dispatchKeyEvent(@NonNull KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			processEntry();
			return true;
		}
		return super.dispatchKeyEvent(e);
	}

	public void processEntry()
	{
		EditText editText = (EditText) findViewById(R.id.editText);
		String ipAddr = editText.getText().toString();
		saveIP(ipAddr);
		validateAndCalculateSubnet(ipAddr);
	}

	// save the IP/mask to the prefs file
	public void saveIP(String ip)
	{
		SharedPreferences sharedPrefs = this.getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(getString(R.string.savedIP), ip);
		editor.apply();
	}


	// 1. check for and validate CIDR notation
	// 2. check for and validate Classful notation
	// 3. calculate subnet info
	// 4. display results
	public void validateAndCalculateSubnet(String ipAddr)
	{
		if (subnet.validateCIDR(ipAddr)) { // handle CIDR notation
			subnet.calculateSubnetCIDR(ipAddr);
		} else if (subnet.validateIPAndMaskOctets(ipAddr)) { // handle IP with mask octets notation
			subnet.calculateSubnetCIDR(subnet.convertToCIDR(ipAddr));
		} else {
			textView.setText("");
			formatWithColors("ERROR: ", "Invalid IP Address / Mask bits", "#0000CC", "#CC0000", true);
			return;
		}
		displayResults();
	}

	// 'CALC' button callback
    public void on_calc(View view)
    {
		processEntry();
    }

    public void format(String title, String result, boolean newline)
    {
        String html = "<pre><b><font color=#000000>" + title + ": </b><font color=#CC0000>" + result + "</font></pre>";
        textView.append(Html.fromHtml(html));
        if (newline) {
            textView.append("\n");
        }
    }

    public void formatWithColors(String title, String result, String color1, String color2, boolean newline)
    {
        String html = "<pre><b><font color=" + color1 + ">" + title + ": </b><font color=" + color2 + ">" + result + "</font></pre>";
        textView.append(Html.fromHtml(html));
        if (newline) {
            textView.append("\n");
        }
    }

	protected void displayLogo()
	{
		String logoString = "<pre><font color=#000000><u><b>Michael</b></u></font>"
				+ "<font color=#c50000><u>Sheppard</u></font>"
				+ "<font color=#0000aa>\u00A0<b>2015</b></font></pre>\n";

		textView.append("\n");
		textView.append("--------------------------\n");

		textView.append(Html.fromHtml(logoString));
	}

    public void lineBreak(boolean line)
    {
        if (line) {
            textView.append("--------------------------\n");
        } else {
            textView.append("\n");
        }
    }

    public void displayResults()
    {
        textView.setText(""); // clear TextView

        format("IPv4 Address", subnet.getIpAddr(), true);
		format("Network Class", subnet.getNetworkClass(), true);
        format("IPv4 Address (hex)", subnet.getIpAddrHex(), true);
        format("Network Mask (bits)", subnet.getNetworkBits(), true);
        format("Network Mask", subnet.getDecimalMaskOctets(), true);
        format("Binary Mask", subnet.getBinaryMaskOctets(), true);
        format("Cisco Wildcard", subnet.getWildcard(), true);
        lineBreak(true);
        format("Max Usable IPs", Integer.toString(subnet.getMaxHosts()), true);
        format("Addresses in Network", Integer.toString(subnet.getNumberOfAddresses()), true);
        lineBreak(true);
		int max_hosts = subnet.calcMaxHosts();

		if (max_hosts > 1) {
			format("Network", subnet.getNetwork(), true);
			format("Broadcast", subnet.getBroadcast(), true);
			format("Available Networks", Integer.toString(subnet.calcAvailableSubnets()), true);
			lineBreak(true);

			// convenience variable
			String max_host_IP = subnet.getMaxHostAddr();

			if (max_hosts <= MAX_HOSTS_TO_DISPLAY) {
				// display the first usable IP
				formatWithColors("Host Address", subnet.getMinHostAddr(), "#005500", "#0000ff", true);
				// calculate the next usable IP
				String next = subnet.getMinHostAddr();
				// the first bitwiseAnd last IPs are already accounted for
				for (int ip = 0; ip < max_hosts - 2; ip++) {
					next = subnet.getNextIPAddress(next);
					if (next.equals(subnet.getIpAddr())) {
						formatWithColors("Host Address", "<b>" + next + "</b>", "#005500", "#0000c5", true);
					} else {
						formatWithColors("Host Address", next, "#005500", "#0000ff", true);
					}
				}
				// display the last usable IP
				formatWithColors("Host Address", max_host_IP, "#005500", "#0000ff", true);
			} else {
				format("Network", subnet.getNetwork() + " - " + subnet.getBroadcast(), true);
				format("Usable", subnet.getMinHostAddr() + " - " + subnet.getMaxHostAddr(), true);
			}

			if (subnet.calcAvailableSubnets() <= 16) {
				lineBreak(true);
				int addrs = subnet.getNumberOfAddresses();
				String[] ranges = subnet.calculateNetworkRanges();
				for (String range : ranges) {
					String[] octets = range.split("[.]");
					octets[3] = Integer.toString((Integer.parseInt(octets[3]) + addrs - 1));
					String endRange = octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
					if (Integer.parseInt(range.split("[.]")[3]) >= 255) {
						break;
					}
					if (range.equals(subnet.getNetwork())) {
						formatWithColors("Range", range + " - " + endRange, "#000000", "#00cc00", true);
					} else {
						formatWithColors("Range", range + " - " + endRange, "#000000", "#cc0000", true);
					}
				}
			}
		} else {
			formatWithColors("Host Address", subnet.getNetwork(), "#005500", "#0000ff", true);
		}

		displayLogo();

        // close the keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
    }
}
