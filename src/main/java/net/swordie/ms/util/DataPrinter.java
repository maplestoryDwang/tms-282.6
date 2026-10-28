package net.swordie.ms.util;

import net.swordie.ms.DiscordAPI;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DataPrinter {

    public static final String
            ALL_IN_ONE = "discord/all.txt",
            REGISTER = "discord/register.txt",
            VOTE = "discord/vote.txt",
            LOGIN = "discord/login.txt",
            LOG_TRADE = "discord/trade.txt",
            LOG_RPS = "discord/rps.txt",
            LOG_CHAT = "discord/chat.txt",
            AUCTION = "discord/auction.txt",
            DONATION_POINT_LOG = "discord/donate.txt",
            AUTOBAN_WARNING = "discord/hack.txt",
            STORAGE = "discord/storage.txt",
            FREEXUVANG = "discord/xuvang.txt",
            ITEM = "Items.txt",
            TIMER = "Timer.txt",
            HIKARICP = "HikariCP/Log.txt",
            HIKARICP_ERROR = "HikariCP/Error.txt",
            EXCEPTION_CAUGHT = "ExceptionCaught/All.txt",
            EXCEPTION_CAUGHT_TIMER = "ExceptionCaught/Timer.txt",
            EXCEPTION_CAUGHT_INPACKET = "ExceptionCaught/InPacket.txt",
            EXCEPTION_CAUGHT_MIGRATION = "ExceptionCaught/Migration.txt",
            EXCEPTION_CAUGHT_SKILL = "ExceptionCaught/Skills.txt",
            EXCEPTION_CAUGHT_ATTACK = "ExceptionCaught/Attacks.txt",
            SCRIPTS = "Scripts.txt",
            AUTOSTART_QUESTS = "AutoStart_Quests.txt",
            CLIENT_ERROR = "Client_Error.txt",
            INSTANCE_FIELD = "Instance_Fields.txt",
            PACKET_LOG = "Packets/",
            PACKET_LOG_ERR = "Packets/errors.txt";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy"); //for file system purposes, it's nice to use yyyy-MM-dd
    private static final SimpleDateFormat sdf4P = new SimpleDateFormat("dd MM yyyy HH:mm:ss"); //for file system purposes, it's nice to use yyyy-MM-dd
    private static final String FILE_PATH = "logs/" + sdf.format(Calendar.getInstance().getTime()) + "/"; // + sdf.format(Calendar.getInstance().getTime()) + "/"

    public static void send(final String name, final Exception e) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        send(name, result.toString(), true);
    }

    public static void send(final String name, final String s) {
        //DiscordAPI.send(name, "``` [" + sdf4P.format(Calendar.getInstance().getTime()) + "] " + s + " ```", DiscordAPI.staffGuildServer);
        System.out.println(s);
        send(name, s, true);
    }

    public static void send(String name, final String s, boolean line) {
        FileOutputStream out = null;
        String result = null;
        String file = FILE_PATH + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            } else {
                outputFile.createNewFile();
            }
            out = new FileOutputStream(file, true);
            result = "[" + sdf4P.format(Calendar.getInstance().getTime()) + "] " + s;
            out.write(result.getBytes());
            if (line) {
                out.write("\r\n---------------------------------\r\n".getBytes());
            } else {
                out.write("\r\n".getBytes());
            }
        } catch (IOException ess) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, ess);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }
}