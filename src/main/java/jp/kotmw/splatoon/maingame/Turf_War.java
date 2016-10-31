package jp.kotmw.splatoon.maingame;

import java.text.DecimalFormat;
import java.util.Random;

import jp.kotmw.splatoon.Main;
import jp.kotmw.splatoon.SplatColor;
import jp.kotmw.splatoon.gamedatas.ArenaData;
import jp.kotmw.splatoon.gamedatas.DataStore;
import jp.kotmw.splatoon.gamedatas.PlayerData;
import jp.kotmw.splatoon.maingame.threads.ResultRunnable;
import jp.kotmw.splatoon.mainweapons.Paint;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;


public class Turf_War {

	public String arena;
	public double result_team1;
	public double result_team2;

	public Turf_War(String arena) {
		this.arena = arena;
	}

	public int getTotalArea() {
		ArenaData data = DataStore.getArenaData(arena);
		int x1 = (int)data.getStagePosition1().getX(), x2 = (int)data.getStagePosition2().getX();
		int y1 = (int)data.getStagePosition1().getY(), y2 = (int)data.getStagePosition2().getY();
		int z1 = (int)data.getStagePosition1().getZ(), z2 = (int)data.getStagePosition2().getZ();
		World world = data.getAreaPosition1().convertLocation().getWorld();
		return getTotalArea(world, x1, x2, y1, y2, z1, z2);
	}

	public static int getTotalArea(World world, int x1, int x2, int y1, int y2, int z1, int z2) {
		int count = 0;
		for(int x = x2; x <= x1; x++) {
			for(int y = y2; y <= y1; y++) {
				for(int z = z2; z <= z1; z++) {
					Block block = world.getBlockAt(x, y, z);
					Block aboveBlock = world.getBlockAt(x, (y+1), z);
					if(block.getType() != Material.AIR
							&& aboveBlock.getType() == Material.AIR)
						if(block.getType() == Material.WOOL
								|| block.getType() == Material.GLASS
								|| block.getType() == Material.THIN_GLASS
								|| block.getType() == Material.HARD_CLAY
								|| block.getType() == Material.STAINED_GLASS
								|| block.getType() == Material.STAINED_GLASS_PANE
								|| block.getType() == Material.STAINED_CLAY
								|| block.getType() == Material.CARPET)
							count++;
				}
			}
		}
		return count;
	}

	public void resultBattle() {
		ArenaData data = DataStore.getArenaData(arena);
		int team1 = 0, team2 = 0;
		byte team1colorID = SplatColor.conversionColorByte(data.getDyeColor(1)),
				team2colorID = SplatColor.conversionColorByte(data.getDyeColor(2));
		Location loc1 = data.getStagePosition1().convertLocation(), loc2 = data.getStagePosition2().convertLocation();
		for(int x = loc2.getBlockX(); x <= loc1.getBlockX(); x++) {
			for(int y = loc2.getBlockY(); y <= loc1.getBlockZ(); y++) {
				for(int z = loc2.getBlockZ(); z <= loc1.getBlockZ(); z++) {
					Block block = Bukkit.getWorld(data.getWorld()).getBlockAt(x, y, z);
					Block aboveBlock = Bukkit.getWorld(data.getWorld()).getBlockAt(x, y+1, z);
					if(block.getType() != Material.AIR
							&& aboveBlock.getType() == Material.AIR) {
						byte colorID = SplatColor.getColorByte(Bukkit.getWorld(data.getWorld()).getBlockAt(x, y, z));
						if(team1colorID == colorID)
							team1++;
						else if(team2colorID == colorID)
							team2++;
					}
				}
			}
		}
		BukkitRunnable task = null;
		result_team1 = team1;
		result_team2 = team2;
		try {
			task = new ResultRunnable(this);
			task.runTaskTimer(Main.main, 20*5, 5);
		} catch (NoClassDefFoundError e) {
			if(task != null)
				task.cancel();
			Bukkit.broadcastMessage(MainGame.Prefix+ChatColor.RED+"重大なエラーが発生したため、エラーの発生したゲームは強制終了し、ロールバックを行います");
			Paint.RollBack(data);
			for(PlayerData player : DataStore.getArenaPlayersList(data.getName())) {
				int i = 0;
				Player p = Bukkit.getPlayer(player.getName());
				for(ItemStack item : player.getRollbackItems()) {
					p.getInventory().setItem(i, item);
					i++;
				}
				p.teleport(player.getRollBackLocation());
			}
		}
	}

	public void sendResult() {
		ArenaData data = DataStore.getArenaData(arena);
		DecimalFormat df = new DecimalFormat("##0.0%");
		double total = data.getTotalpaintblock();
		double parce_team1 = (result_team1/total);
		double parce_team2 = (result_team2/total);
		if(parce_team1 == parce_team2) {
			Random random = new Random();
			int randomteam = random.nextInt(2);
			if(randomteam == 1)
				parce_team1+=0.01f;
			else if(randomteam == 2)
				parce_team2+=0.01f;
		}
		String result = "[ "+SplatColor.conversionChatColor(data.getDyeColor(1))+df.format(parce_team1)+ChatColor.WHITE+" ]      [ "
		+SplatColor.conversionChatColor(data.getDyeColor(2))+df.format(parce_team2)+ChatColor.WHITE+" ]";
		String win = ChatColor.GOLD.toString()+ChatColor.BOLD+"You Win!";
		String lose = ChatColor.BLUE.toString()+ChatColor.ITALIC+"You Lose...";
		MainGame.sendTitleforTeam(data, 1, 0, 5, 0, parce_team1 > parce_team2 ? win : lose, result);
		MainGame.sendTitleforTeam(data, 2, 0, 5, 0, parce_team1 > parce_team2 ? lose : win, result);
	}
}
