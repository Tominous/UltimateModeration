package com.songoda.ultimatemoderation.gui;

import com.songoda.ultimatemoderation.UltimateModeration;
import com.songoda.ultimatemoderation.tickets.Ticket;
import com.songoda.ultimatemoderation.tickets.TicketResponse;
import com.songoda.ultimatemoderation.tickets.TicketStatus;
import com.songoda.ultimatemoderation.utils.AbstractChatConfirm;
import com.songoda.ultimatemoderation.utils.gui.AbstractAnvilGUI;
import com.songoda.ultimatemoderation.utils.gui.AbstractGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class GUITicket extends AbstractGUI {

    private final UltimateModeration plugin;

    private final Ticket ticket;

    private final OfflinePlayer toModerate;
    private int page = 0;

    public GUITicket(UltimateModeration plugin, Ticket ticket, OfflinePlayer toModerate, Player player) {
        super(player);
        this.ticket = ticket;
        this.plugin = plugin;
        this.toModerate = toModerate;

        init(plugin.getLocale().getMessage("gui.ticket.title", ticket.getTicketId()), 54);
    }

    @Override
    protected void constructGUI() {
        inventory.clear();
        resetClickables();
        registerClickables();

        int numNotes = ticket.getResponses().size();
        int maxPage = (int) Math.floor(numNotes / 36.0);

        List<TicketResponse> responses = ticket.getResponses().stream().skip(page * 36).limit(36)
                .collect(Collectors.toList());

        if (page != 0) {
            createButton(1, Material.ARROW, plugin.getLocale().getMessage("gui.general.previous"));
            registerClickable(1, ((player1, inventory1, cursor, slot, type) -> {
                page --;
                constructGUI();
            }));
        }

        if (page != maxPage) {
            createButton(3, Material.ARROW, plugin.getLocale().getMessage("gui.general.next"));
            registerClickable(3, ((player1, inventory1, cursor, slot, type) -> {
                page ++;
                constructGUI();
            }));
        }

        if (player.hasPermission("um.ticket.openclose"))
            createButton(5, Material.REDSTONE, "&6" + ticket.getStatus().getStatus());

        createButton(8, Material.OAK_DOOR, plugin.getLocale().getMessage("gui.general.back"));

        if (player.hasPermission("um.ticket.clicktotele") && ticket.getLocation() != null)
            createButton(7, Material.REDSTONE, plugin.getLocale().getMessage("gui.ticket.clicktotele"));

        createButton(6, Material.REDSTONE,  plugin.getLocale().getMessage("gui.ticket.respond"));

        for (int i = 0; i < 9; i++)
            createButton(9 + i, Material.GRAY_STAINED_GLASS_PANE, "&1");

        for (int i = 0; i < responses.size(); i++) {
            TicketResponse ticketResponse = responses.get(i);

            String subjectStr = ticketResponse.getMessage();

            ArrayList<String> lore = new ArrayList<>();
            int lastIndex = 0;
            for (int n = 0; n < subjectStr.length(); n++) {
                if (n - lastIndex < 20)
                    continue;

                if (subjectStr.charAt(n) == ' ') {
                    lore.add("&6" +subjectStr.substring(lastIndex, n).trim());
                    lastIndex = n;
                }
            }

            if (lastIndex - subjectStr.length() < 20)
                lore.add("&6" + subjectStr.substring(lastIndex, subjectStr.length()).trim());

            String name = lore.get(0);
            lore.remove(0);

            lore.add("");

            SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");


            lore.add(plugin.getLocale().getMessage("gui.ticket.postedby", Bukkit.getOfflinePlayer(ticketResponse.getAuthor()).getName() ));
            lore.add(plugin.getLocale().getMessage("gui.ticket.createdon", format.format(new Date(ticketResponse.getPostedDate()))));

            createButton(18 + i, Material.MAP, name, lore);
        }
    }

    @Override
    protected void registerClickables() {
        registerClickable(8, ((player1, inventory1, cursor, slot, type) ->
                new GUITicketManager(plugin, toModerate, player)));

        if (player.hasPermission("um.ticket.clicktotele") && ticket.getLocation() != null) {
            registerClickable(7, ((player1, inventory1, cursor, slot, type) ->
                    player.teleport(ticket.getLocation())));
        }

        if (player.hasPermission("um.ticket.openclose")) {
            registerClickable(5, ((player1, inventory1, cursor, slot, type) -> {
                    ticket.setStatus(ticket.getStatus() == TicketStatus.OPEN ? TicketStatus.CLOSED : TicketStatus.OPEN);
                    constructGUI();
            }));
        }

        registerClickable(6, ((player1, inventory1, cursor, slot, type) -> {
            player.sendMessage(plugin.getLocale().getMessage("gui.ticket.what"));
            AbstractChatConfirm abstractChatConfirm = new AbstractChatConfirm(player, event2 -> {
                ticket.addResponse(new TicketResponse(player, event2.getMessage(), System.currentTimeMillis()));
                constructGUI();
            });

            abstractChatConfirm.setOnClose(() ->
                    init(inventory.getTitle(), inventory.getSize()));
        }));
    }

    @Override
    protected void registerOnCloses() {
    }
}
