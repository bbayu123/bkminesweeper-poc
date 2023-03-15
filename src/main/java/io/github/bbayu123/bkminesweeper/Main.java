/* **********************************************************************
 * Copyright (C) 2023 Cyrus Mian Xi Li (bbayu/bbayu123)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * **********************************************************************
 */
package io.github.bbayu123.bkminesweeper;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.events.map.MapClickEvent;
import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.events.map.MapStatusEvent;
import com.bergerkiller.bukkit.common.map.MapCanvas;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapDisplayProperties;
import com.bergerkiller.bukkit.common.map.MapEventPropagation;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.MapPlayerInput;
import com.bergerkiller.bukkit.common.map.MapSessionMode;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.util.MapLookPosition;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetButton;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetText;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.resources.SoundEffect;
import com.bergerkiller.bukkit.common.utils.ItemUtil;

/**
 * This is the main plugin class
 * <p>
 * Contains boiler-plate code for the plugin to work.
 *
 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
 *
 */
public class Main extends JavaPlugin {
	private MapTexture flagTextureNormal = null, flagTextureTiny = null;

	/**
	 * {@inheritDoc}
	 * <p>
	 * What we are doing here is linking the command executor to our plugin, as well
	 * as loading some map textures that we will be using later. See
	 * {@link #loadTexture(String)} as to how the textures are loaded.
	 */
	@Override
	public void onEnable() {
		this.getCommand("minesweeper").setExecutor(this);

		try {
			this.flagTextureNormal = this.loadTexture("icons/flag_normal.png");
			this.flagTextureTiny = this.loadTexture("icons/flag_tiny.png");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Since only players can hold items, we check if it is a player before
	 * continuing.
	 * <p>
	 * When the player does {@code /minesweeper get}, then we create the map item
	 * using {@link MapDisplay#createMapItem(Class)}, and give this to the player.
	 * <p>
	 * If we need to pass in parameters/properties to the display, we use
	 * {@link ItemUtil#getMetaTag(ItemStack)} and then call
	 * {@link CommonTagCompound#putValue(String, Object) putValue(String, Object)}
	 * on the tag to add properties.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player in order to do this!");
			return true;
		}

		Player player = (Player) sender;

		if (args.length == 0) {
			return false;
		}
		if (args[0].equalsIgnoreCase("get")) {
			ItemStack item = MapDisplay.createMapItem(Minesweeper.class);
			ItemUtil.getMetaTag(item).putValue("owner", player.getUniqueId());
			ItemUtil.setDisplayName(item, "Minesweeper");
			player.getInventory().addItem(item);
			player.sendMessage(ChatColor.GREEN + "Obtained Minesweeper");
		}
		return true;
	}

	/**
	 * Loads a {@link MapTexture} from the given resource location
	 * <p>
	 * There are multiple ways to load textures other than the current one. Check
	 * below for a list of valid methods of loading a texture. Our implementation
	 * uses {@link MapTexture#loadPluginResource(JavaPlugin, String)}.
	 *
	 * @param filename the name of the resource file
	 * @return the {@code MapTexture} loaded
	 * @throws IOException when the texture fails to load
	 *
	 * @see MapTexture#loadPluginResource(JavaPlugin, String)
	 * @see MapTexture#loadResource(Class, String)
	 * @see MapTexture#loadResource(java.net.URL)
	 * @see MapTexture#fromImageFile(String)
	 * @see MapTexture#fromImage(java.awt.Image)
	 * @see MapTexture#fromStream(InputStream)
	 * @see MapTexture#fromBukkitSprite(org.bukkit.map.MapFont.CharacterSprite)
	 * @see MapTexture#fromRawData(int, int, byte[])
	 */
	private MapTexture loadTexture(String filename) throws IOException {
		return MapTexture.loadPluginResource(this, filename);
	}

	/**
	 * A simple check to see if the clicked position is within the bounds of the
	 * target widget
	 *
	 * @param widget   the target widget that we want to check bounds for
	 * @param clickedX the X-position of the click
	 * @param clickedY the Y-position of the click
	 * @return whether the click was within the bounds of the widget, or
	 *         {@code false} if the widget is {@code null}
	 *
	 * @see MapWidget#getAbsoluteX()
	 * @see MapWidget#getAbsoluteY()
	 * @see MapWidget#getWidth()
	 * @see MapWidget#getHeight()
	 */
	public static boolean isInBounds(MapWidget widget, int clickedX, int clickedY) {
		if (widget == null) {
			return false;
		}

		return clickedX >= widget.getAbsoluteX() && clickedX <= widget.getAbsoluteX() + widget.getWidth()
				&& clickedY >= widget.getAbsoluteY() && clickedY <= widget.getAbsoluteY() + widget.getHeight();
	}

	/**
	 * Gets the normal flag texture
	 * <p>
	 * This is the texture that was loaded in {@link #onEnable()}.
	 *
	 * @return the normal flag texture
	 */
	public MapTexture getFlagTextureNormal() {
		return this.flagTextureNormal;
	}

	/**
	 * Gets the tiny flag texture
	 * <p>
	 * This is the texture that was loaded in {@link #onEnable()}.
	 *
	 * @return the tiny flag texture
	 */
	public MapTexture getFlagTextureTiny() {
		return this.flagTextureTiny;
	}

	/**
	 * This is the main driver class for the Minesweeper game
	 * <p>
	 * This is a {@link MapDisplay} class. Use this class to understand how to use
	 * {@code MapDisplay}s.
	 * <p>
	 * It is important to note that this class must have {@code public} visibility.
	 * Having {@code protected}, package (default), or {@code private} visibility
	 * will not work.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 *
	 */
	public static class Minesweeper extends MapDisplay {
		/**
		 * Whether walking by sneaking is enabled or not.
		 */
		private boolean sneakWalking = false;

		/**
		 * Holds the minesweeper board.
		 */
		private MinesweeperBoard board = null;

		/**
		 * The owner of this minesweeper display. Unused in this example.
		 */
		@SuppressWarnings("unused")
		private UUID owner = null;

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to initialize a {@code MapDisplay}.
		 * <p>
		 * <b>We do not use the constructor to initialize a {@code MapDisplay}.</b>
		 * <p>
		 * If we have passed properties to the display, we use the {@code properties}
		 * object and call {@link MapDisplayProperties#get(String, Class) get(String,
		 * Class)} to re-call them.
		 * <p>
		 * This method only sets up the behavior of the display. We use a separate
		 * method to handle the content of the display.
		 *
		 * @see {@link MapDisplay#properties} for more information about the properties
		 *      object
		 * @see {@link MapDisplayProperties#get(String, Class)} to get properties that
		 *      were stored
		 * @see {@link #reload()} for more information on how the content is handled
		 */
		@Override
		public void onAttached() {
			this.owner = this.properties.get("owner", UUID.class);

			this.setGlobal(true);
			this.setUpdateWithoutViewers(false);
			this.setSessionMode(MapSessionMode.VIEWING);
			this.setMasterVolume(0.3f);
			this.reload();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to do 2 things:
		 * <ol>
		 * <li>Override map input to allow walking while sneaking
		 * <li>Update child widgets with the current position on the display that the
		 * player is looking at
		 * </ol>
		 */
		@Override
		public void onTick() {
			if (this.getViewers().size() == 0) {
				return;
			}
			Player player = this.getViewers().get(0);

			// Allow walking around when sneaking
			if (this.sneakWalking && !player.isSneaking()) {
				this.sneakWalking = false;
				this.setReceiveInputWhenHolding(true);
			}

			// Update child widgets with hover position
			MapLookPosition lookPosition = this.findLookPosition(player);
			if (lookPosition != null) {
				this.board.sendStatusChange(MapEventPropagation.UPSTREAM, "HOVER",
						new Point(lookPosition.getX(), lookPosition.getY()));
			}
		}

		/**
		 * Reloads the contents of this display
		 * <p>
		 * Here, we do several things:
		 * <ol>
		 * <li>Clear all existing widgets
		 * <li>Initialize the minesweeper board widget
		 * <li>Fill the background with a gray color
		 * <li>Add the board widget to the display
		 * <li>Update sneak walking
		 * </ol>
		 *
		 * @see {@link MapDisplay#clearWidgets()} to clear all widgets
		 * @see {@link MapDisplay#getLayer()} to get layer 0 (which is usually the
		 *      background)
		 * @see {@link Layer#fillRectangle(int, int, int, int, byte)} to fill a
		 *      rectangle
		 * @see {@link MapColorPalette#getColor(int, int, int)} to get a byte color
		 *      representation
		 * @see {@link MapDisplay#addWidget(MapWidget)} to add the widget to the display
		 */
		public void reload() {
			this.clearWidgets();

			this.board = new MinesweeperBoard();
			this.board.setState(GameState.TITLE);

			this.getLayer().fillRectangle(0, 0, this.getWidth(), this.getHeight(),
					MapColorPalette.getColor(223, 223, 223));
			this.addWidget(this.board);

			this.sneakWalking = this.getOwners().get(0).isSneaking();
			this.setReceiveInputWhenHolding(!this.sneakWalking);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to update sneak walking.
		 *
		 * @see #updateSneakWalking(MapKeyEvent)
		 */
		@Override
		public void onKeyPressed(MapKeyEvent event) {
			super.onKeyPressed(event);
			this.updateSneakWalking(event);
		}

		/**
		 * Updates sneak walking based on the received key event
		 *
		 * @param event the key press event that was received
		 * @return whether sneak walking should be allowed or not
		 */
		private boolean updateSneakWalking(MapKeyEvent event) {
			if (event.getKey() == MapPlayerInput.Key.BACK) {
				this.setReceiveInputWhenHolding(false);
				this.getOwners().get(0).setSneaking(true);
				this.sneakWalking = true;
				return true;
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this to cancel the default behavior of the click event, and notify the
		 * clicked position to all child widgets.
		 *
		 * @param event the event containing the map click
		 * @see {@link MapDisplay#sendStatusChange(String, Object)} for notifying the
		 *      entire display
		 * @see {@link MapWidget#sendStatusChange(MapEventPropagation, String, Object)}
		 *      for notifying a specific widget with a direction to propagate the
		 *      message
		 * @see {@link #onRightClick(MapClickEvent)} for the exact same thing but
		 *      handling right-click instead
		 */
		@Override
		public void onLeftClick(MapClickEvent event) {
			if (event.getPlayer().isSneaking()) {
				return;
			}
			event.setCancelled(true);
			this.board.sendStatusChange(MapEventPropagation.UPSTREAM, "LEFT_CLICK",
					new Point(event.getX(), event.getY()));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this to cancel the default behavior of the click event, and notify the
		 * clicked position to all child widgets.
		 *
		 * @param event the event containing the map click
		 * @see {@link MapDisplay#sendStatusChange(String, Object)} for notifying the
		 *      entire display
		 * @see {@link MapWidget#sendStatusChange(MapEventPropagation, String, Object)}
		 *      for notifying a specific widget with a direction to propagate the
		 *      message
		 * @see {@link #onLeftClick(MapClickEvent)} for the exact same thing but
		 *      handling left-click instead
		 */
		@Override
		public void onRightClick(MapClickEvent event) {
			if (event.getPlayer().isSneaking()) {
				return;
			}
			event.setCancelled(true);
			this.board.sendStatusChange(MapEventPropagation.UPSTREAM, "RIGHT_CLICK",
					new Point(event.getX(), event.getY()));
		}
	}

	/**
	 * This is the board widget class for the Minesweeper game
	 * <p>
	 * This is a {@link MapWidget} class. Use this class to understand how to use
	 * {@code MapWidget}s.
	 * <p>
	 * This widget handles all the game logic of minesweeper. The appearance and
	 * visuals are handled by each child widget.
	 * <p>
	 * This class can have any visibility, as long as it is visible to the display
	 * class.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 *
	 * @see {@link MinesweeperTile} for another widget class
	 */
	private static class MinesweeperBoard extends MapWidget {
		/**
		 * The minimum border thickness
		 */
		private static final int MINIMUM_BORDER = 1;

		/**
		 * A list of number pairs that are considered relatively adjacent
		 */
		private static final List<Point> ADJACENTS = Arrays.asList(new Point(-1, -1), new Point(0, -1),
				new Point(1, -1), new Point(-1, 0), new Point(1, 0), new Point(-1, 1), new Point(0, 1),
				new Point(1, 1));

		/**
		 * The current state of the game
		 */
		private GameState state = GameState.TITLE;

		/**
		 * The difficulty selected
		 */
		private MinesweeperDifficulty difficulty = null;

		/**
		 * The list of minesweeper tiles on the board
		 */
		private List<MinesweeperTile> tiles = null;

		/**
		 * Whether this is the first click or not. There is extra logic when this is the
		 * first click of the game.
		 */
		private boolean firstClick = false;

		/**
		 * The time when the game started
		 */
		private ZonedDateTime startTime = null;

		/**
		 * The number of times the board was regenerated
		 */
		private int boardRegenerationCount = 0;
		/**
		 * The index of the tile to delay uncover until the next tick
		 */
		private int delayUncoverTileIndex = -1;

		/**
		 * Creates a MinesweeperBoard
		 * <p>
		 * Here we ensure that the board is focusable, which is required to allow key
		 * presses to be received without any focusable child widgets.
		 */
		public MinesweeperBoard() {
			this.setFocusable(true);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Here we set the bounds of the widget, and initiate the loading of the board.
		 *
		 * @see {@link #reload()} for more information on how the board is loaded
		 */
		@Override
		public void onAttached() {
			super.onAttached();
			this.setBounds(MinesweeperBoard.MINIMUM_BORDER, MinesweeperBoard.MINIMUM_BORDER,
					this.display.getWidth() - MinesweeperBoard.MINIMUM_BORDER * 2,
					this.display.getHeight() - MinesweeperBoard.MINIMUM_BORDER * 2);
			this.reload();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * After regenerating the board, we cannot immediately uncover a tile, as the
		 * tile is not attached yet. We use this method to delay the uncover until the
		 * next map update cycle.
		 */
		@Override
		public void onTick() {
			if (this.delayUncoverTileIndex != -1) {
				int temp = this.delayUncoverTileIndex;
				this.delayUncoverTileIndex = -1;
				this.getTile(temp).tryUncover();
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In here, we draw 2 lines of text within the widget bounds using different
		 * fonts.
		 *
		 * @see {@link MapWidget#view} for obtaining the canvas that is bound by the
		 *      widget
		 * @see {@link MapCanvas#draw(MapFont, int, int, byte, CharSequence)} for
		 *      drawing the text onto the canvas
		 */
		@Override
		public void onDraw() {
			if (this.tiles != null) {
				return;
			}

			MapFont<Character> titleFont = MapFont.MINECRAFT, subtitleFont = MapFont.TINY;
			String titleText = "MINESWEEPER", subtitleText = "TAP TO START";
			byte textColor = MapColorPalette.getColor(0, 0, 128);
			byte subColor = MapColorPalette.getSpecular(textColor, 0.7f);

			Dimension titleDimensions = this.view.calcFontSize(titleFont, titleText);
			Dimension subtitleDimensions = this.view.calcFontSize(subtitleFont, subtitleText);

			int titleX = (this.getWidth() - titleDimensions.width) / 2;
			int titleY = this.getHeight() / 2 - 4 - titleDimensions.height;
			int subtitleX = (this.getWidth() - subtitleDimensions.width) / 2;
			int subtitleY = this.getHeight() / 2 + 4;

			this.view.draw(titleFont, titleX + 1, titleY + 1, subColor, titleText);
			this.view.draw(subtitleFont, subtitleX + 1, subtitleY + 1, subColor, subtitleText);
			this.view.draw(titleFont, titleX, titleY, textColor, titleText);
			this.view.draw(subtitleFont, subtitleX, subtitleY, textColor, subtitleText);
		}

		/**
		 * Reloads the widget
		 * <p>
		 * What we do here is:
		 * <ol>
		 * <li>Clear all existing widgets
		 * <li>Load the required board state
		 * <li>Draw the board to the root widget
		 * </ol>
		 *
		 * @see {@link MapWidget#clearWidgets()} for clearing all widgets
		 * @see {@link #loadBoard()} for loading the required board state
		 * @see {@link #drawBoard()} for drawing the board to the root widget
		 */
		public void reload() {
			if (this.display == null) {
				return;
			}

			this.clearWidgets();
			this.loadBoard();
			this.drawBoard();
		}

		/**
		 * Loads the required state of the board
		 * <p>
		 * This method does different things depending on the current state of the game.
		 * <table border="1">
		 * <tr>
		 * <th>{@link GameState}
		 * <th>Action
		 * <tr>
		 * <td>{@code TITLE}
		 * <td>Resets the difficulty, tiles list, and start time
		 * <tr>
		 * <td>{@code GAME}
		 * <td>Generates a new board and sets the first-click flag if a difficulty is
		 * set and no tiles are generated; does nothing otherwise
		 * <tr>
		 * <td>{@code WIN}
		 * <td>Flags all non-flagged mines, and opens the win/lose dialog
		 * <tr>
		 * <td>{@code LOSE}
		 * <td>Opens the win/lose dialog
		 * </table>
		 *
		 * @see {@link #generateNewBoard()} for more information on how a new board is
		 *      generated
		 * @see {@link #openWinLoseDialog(boolean)} for more information on how the
		 *      win/lose dialog is opened
		 */
		private void loadBoard() {
			switch (this.state) {
			case TITLE: {
				this.difficulty = null;
				this.tiles = null;
				this.startTime = null;
				break;
			}
			case GAME: {
				if (this.difficulty != null && this.tiles == null) {
					// New Game
					this.generateNewBoard(null);
					this.firstClick = true;
				}
				break;
			}
			case WIN: {
				this.tiles.stream().filter(MinesweeperTile::isMine).filter(tile -> !tile.isFlagged())
						.forEach(tile -> tile.setFlagged(true));
				this.openWinLoseDialog(true);
				break;
			}
			case LOSE: {
				this.openWinLoseDialog(false);
				break;
			}
			}
		}

		/**
		 * Draws the board to the current root widget
		 * <p>
		 * This method adds tiles for each in the list, and sets the initial focused
		 * tile to be the one in the middle.
		 */
		private void drawBoard() {
			if (this.tiles == null) {
				return;
			}

			for (MinesweeperTile tile : this.tiles) {
				this.addWidget(tile);
			}

			// Set focus in the middle
			final int tileRows = this.difficulty.rows();
			final int tileCols = this.difficulty.cols();

			this.getTile(tileRows / 2, tileCols / 2).focus();
		}

		/**
		 * Generates a new board
		 * <p>
		 * In here, we do 3 things:
		 * <ol>
		 * <li>Generate all the required tiles, setting their bounds and focusable
		 * state, and hook into any callback functions that are required
		 * <li>Randomly pick tiles to generate the required number of mines. This loops
		 * infinitely until the required number is achieved
		 * <li>For each non-mine, count the number of mines surrounding it, and update
		 * the number that's shown on the tile
		 *
		 * @param positionToAvoid the position to avoid placing mines, if any
		 */
		private void generateNewBoard(Point positionToAvoid) {
			final int tileRows = this.difficulty.rows();
			final int tileCols = this.difficulty.cols();
			final int tileWidth = this.getWidth() / tileCols;
			final int tileHeight = this.getHeight() / tileRows;

			final Random random = new Random();

			this.tiles = new ArrayList<>();

			/*
			 * 1. Generate tiles
			 *
			 * a. Create each tile, and hook any callback functions needed.
			 *
			 * b. Set the bounds of each tile, make them focusable, and add them to the tile
			 * list.
			 */
			for (int row = 0; row < tileRows; row++) {
				for (int col = 0; col < tileCols; col++) {
					MinesweeperTile tile = new MinesweeperTile() {
						@Override
						public void onUncover() {
							MinesweeperBoard.this.handleTileUncover(this);
						}

						@Override
						public void onTileNavigate(MapKeyEvent event) {
							MinesweeperBoard.this.handleTileNavigation(this, event.getKey());
						}
					};
					tile.setBounds(tileWidth * col, tileHeight * row, tileWidth, tileHeight);
					tile.setFocusable(true);
					this.tiles.add(tile);
				}
			}

			/*
			 * 2. Generate mines
			 *
			 * While the number of mines is less than required:
			 *
			 * a. Generate a random number representing the target tile
			 *
			 * b. Set the tile as a mine if it is not already a mine
			 *
			 * Note that this is written as a for-loop, but acts as a while loop.
			 */
			loop: for (int generated = 0; generated < this.difficulty.mines();) {
				int index = random.nextInt(this.tiles.size());
				Point rowCol = this.getRowColFromIndex(index);

				if (positionToAvoid != null) {
					if (rowCol.equals(positionToAvoid)) {
						continue;
					}
					for (Point adjacent : MinesweeperBoard.ADJACENTS) {
						if (rowCol.equals(new Point(positionToAvoid.x + adjacent.x, positionToAvoid.y + adjacent.y))) {
							continue loop;
						}
					}
				}

				MinesweeperTile tile = this.getTile(index);

				if (tile.isMine()) {
					continue;
				}
				tile.setMine(true);
				generated++;
			}

			/*
			 * 3. Fill numbers
			 *
			 * For each non-mine tile in the list:
			 *
			 * a. Count the number of mines surrounding the tile
			 *
			 * b. Set the mine count to the tile
			 */
			for (int row = 0; row < tileRows; row++) {
				for (int col = 0; col < tileCols; col++) {
					MinesweeperTile tile = this.getTile(row, col);
					if (tile.isMine()) {
						continue;
					}

					// Count the number of mines surrounding the tile
					int mineCount = 0;
					for (Point adjacent : MinesweeperBoard.ADJACENTS) {
						int newRow = row + adjacent.x, newCol = col + adjacent.y;
						MinesweeperTile newTile = this.getTile(newRow, newCol);
						if (newTile == null) {
							continue;
						}

						if (newTile.isMine()) {
							mineCount++;
						}
					}

					// Set the mine count to the tile
					tile.setValue(mineCount);
				}
			}
		}

		/**
		 * Handles the logic when a tile is uncovered
		 * <p>
		 * Read the comments that are inserted within the code to understand how this
		 * works.
		 *
		 * @param uncovered the tile that was uncovered
		 */
		private void handleTileUncover(MinesweeperTile uncovered) {
			if (this.firstClick) {
				/*
				 * First tile uncovered logic
				 *
				 * If first click is a mine, or a non-zero, re-generate board and re-click
				 */
				if (uncovered.isMine() || uncovered.getValue() > 0 && this.boardRegenerationCount < 10) {
					// Regenerate the board, and uncover the tile again
					int index = this.tiles.indexOf(uncovered);
					this.generateNewBoard(this.getRowColFromIndex(index));
					this.reload();
					this.boardRegenerationCount++;
					this.delayUncoverTileIndex = index;
					return;
				}
				// First click is valid, start the timer and unset the first-click flag
				this.firstClick = false;
				this.startTime = ZonedDateTime.now();
			}

			/*
			 * Regular uncover logic
			 */
			// If any mine is uncovered, mark as lose
			if (this.tiles.stream().filter(MinesweeperTile::isMine).anyMatch(tile -> !tile.isCovered())) {
				this.setState(GameState.LOSE);
				return;
			}

			// If all non-mines are uncovered, mark as win
			if (this.tiles.stream().filter(tile -> !tile.isMine()).allMatch(tile -> !tile.isCovered())) {
				this.setState(GameState.WIN);
				return;
			}

			// If uncovered is 0, uncover all 8 adjacent cells
			if (uncovered.getValue() == 0) {
				Point rowCol = this.getRowColFromIndex(this.tiles.indexOf(uncovered));

				for (Point adjacent : MinesweeperBoard.ADJACENTS) {
					int newRow = rowCol.x + adjacent.x;
					int newCol = rowCol.y + adjacent.y;
					MinesweeperTile newTile = this.getTile(newRow, newCol);
					if (newTile == null) {
						continue;
					}

					if (newTile.isCovered()) {
						newTile.tryUncover();
					}
				}
			}
		}

		/**
		 * Handles the keyboard navigation logic
		 * <p>
		 * Normally this isn't required, but because of how the tiles are positioned,
		 * the default logic moves the selection up/left from the intended selection.
		 * <p>
		 * As such, this method handles tile navigation in a different way compared to
		 * the default navigation
		 *
		 * @param currentTile the tile being navigated away from
		 * @param pressed     the input key that was pressed
		 *
		 * @see {@link MapWidget#handleNavigation} for the default implementation of
		 *      navigation
		 */
		private void handleTileNavigation(MinesweeperTile currentTile, MapPlayerInput.Key pressed) {
			Point rowCol = this.getRowColFromIndex(this.tiles.indexOf(currentTile));
			int row = rowCol.x, col = rowCol.y;
			int totalRows = this.difficulty.rows(), totalCols = this.difficulty.cols();

			switch (pressed) {
			case UP: {
				if (row - 1 >= 0) {
					this.getTile(row - 1, col).focus();
				}
				break;
			}
			case DOWN: {
				if (row + 1 < totalRows) {
					this.getTile(row + 1, col).focus();
				}
				break;
			}
			case LEFT: {
				if (col - 1 >= 0) {
					this.getTile(row, col - 1).focus();
				}
				break;
			}
			case RIGHT: {
				if (col + 1 < totalCols) {
					this.getTile(row, col + 1).focus();
				}
				break;
			}
			default:
				break;
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to open the difficulty selector when a left-click action
		 * is received and there are no child widgets on the board.
		 * <p>
		 * It is important that the status event is sent to the {@code MapWidget} parent
		 * ({@code super}) if the event is not handled, otherwise it will not propagate.
		 */
		@Override
		public void onStatusChanged(MapStatusEvent event) {
			if (this.getWidgetCount() == 0 && event.getName().equals("LEFT_CLICK")) {
				this.openDifficultySelector();
				return;
			}

			super.onStatusChanged(event);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to open the difficulty selector when the {@code ENTER} key
		 * is received and there are no child widgets on the board.
		 * <p>
		 * It is important that the key press event is sent to the {@code MapWidget}
		 * parent ({@code super}) if the event is not handled, otherwise it will not
		 * propagate.
		 */
		@Override
		public void onKeyPressed(MapKeyEvent event) {
			if (this.getWidgetCount() == 0 && event.getKey() == MapPlayerInput.Key.ENTER) {
				this.openDifficultySelector();
				return;
			}

			super.onKeyPressed(event);
		}

		/**
		 * Opens the difficulty selector
		 * <p>
		 * This method creates the difficulty selector, hooking into any callback
		 * methods as required, and adds it as a widget to the board.
		 *
		 * @see MinesweeperDifficultySelectDialog
		 */
		private void openDifficultySelector() {
			this.addWidget(new MinesweeperDifficultySelectDialog() {
				@Override
				public void onSubmit() {
					MinesweeperBoard.this.difficulty = this.selected;
					MinesweeperBoard.this.setState(GameState.GAME);
				}
			});
		}

		/**
		 * Opens the win/lose dialog
		 * <p>
		 * This method creates the win/lose dialog, passing any arguments as needed and
		 * hooking into any callback methods as required, and adds it as a widget to the
		 * board. It also sets all tiles to be non-focusable.
		 *
		 * @param win whether the game ended in a win or not
		 * @see MinesweeperWinLoseDialog
		 */
		private void openWinLoseDialog(boolean win) {
			Duration time = Duration.between(this.startTime, ZonedDateTime.now());
			int numMines = this.difficulty.mines();
			int flags = win ? numMines : this.tiles.stream().filter(MinesweeperTile::isFlagged).mapToInt(e -> 1).sum();

			this.addWidget(new MinesweeperWinLoseDialog(win, time, flags, numMines) {
				@Override
				public void onClose() {
					MinesweeperBoard.this.setState(GameState.TITLE);
				}
			});

			this.tiles.forEach(tile -> tile.setFocusable(false));
		}

		/**
		 * Sets the current game state
		 * <p>
		 * Setting the game state triggers a board reload.
		 *
		 * @param newState the new game state
		 */
		public void setState(GameState newState) {
			this.state = newState;
			this.reload();
		}

		/**
		 * Gets a tile from its row,col location
		 *
		 * @param row the row of the tile
		 * @param col the column of the tile
		 * @return the requested tile, or null if there are no tiles or the selection is
		 *         out of bounds
		 */
		private MinesweeperTile getTile(int row, int col) {
			if (this.difficulty == null || this.tiles == null) {
				return null;
			}
			if (row < 0 || row >= this.difficulty.rows() || col < 0 || col >= this.difficulty.cols()) {
				return null;
			}
			return this.tiles.get(row * this.difficulty.cols() + col);
		}

		/**
		 * Gets a tile from its index
		 *
		 * @param index the index of the tile
		 * @return the requested tile, or null if there are no tiles or the selection is
		 *         out of bounds
		 */
		private MinesweeperTile getTile(int index) {
			if (this.difficulty == null || this.tiles == null) {
				return null;
			}
			return this.tiles.get(index);
		}

		/**
		 * Calculates the row and column of a tile with a given index
		 *
		 * @param index the index of the tile
		 * @return the row and column of the tile
		 */
		private Point getRowColFromIndex(int index) {
			if (this.difficulty == null || this.tiles == null || index < 0 || index >= this.tiles.size()) {
				return null;
			}
			return new Point(index / this.difficulty.cols(), index % this.difficulty.cols());
		}
	}

	/**
	 * This is the tile widget class for the Minesweeper game
	 * <p>
	 * This is a {@link MapWidget} class. Use this class to understand how to use
	 * {@code MapWidget}s.
	 * <p>
	 * This widget handles the appearance and visuals for each minesweeper tile. The
	 * game logic is handled in a different widget.
	 * <p>
	 * This class can have any visibility, as long as it is visible to the display
	 * class.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 *
	 * @see {@link MinesweeperBoard} for the game logic widget
	 */
	private static class MinesweeperTile extends MapWidget {
		/**
		 * Whether the tile contains a mine or not
		 */
		private boolean mine = false;
		/**
		 * The numeric value of a tile
		 */
		private int value = 0;

		/**
		 * Whether the tile is flagged or not
		 */
		private boolean flagged = false;
		/**
		 * Whether the tile is covered or not
		 */
		private boolean covered = true;

		/**
		 * A pseudo-state indicating the tile is pressed. Used for visually representing
		 * a pressed tile
		 */
		private boolean pressed = false;
		/**
		 * A pseudo-state indicating the tile is focused. Used for visually representing
		 * a focused tile
		 */
		private boolean focused = false;

		/**
		 * {@inheritDoc}
		 * <p>
		 * Here, the drawing routine is as follows:
		 * <ol>
		 * <li>Draw a rectangle border that is the size of the tile
		 * <li>If the tile is covered:
		 * <ol type="a">
		 * <li>Fill the area with the cover color
		 * <li>Add the flag icon if the tile is flagged. The flag icon is obtained from
		 * the {@link Main} class.
		 * </ol>
		 * Otherwise:
		 * <ol type="a">
		 * <li>Fill the area with the background color
		 * <li>Add an icon/character representing the tile, either a mine, a number, or
		 * nothing if no mines surround the tile
		 * </ol>
		 * </ol>
		 *
		 * @see {@link MapColorPalette#getColor(int, int, int)} for getting a byte color
		 *      from RGB
		 * @see {@link MapWidget#view} for obtaining the canvas that is bound by the
		 *      widget
		 * @see {@link MapCanvas#drawRectangle(int, int, int, int, byte)} for drawing a
		 *      bordered rectangle
		 * @see {@link MapCanvas#fillRectangle(int, int, int, int, byte)} for drawing a
		 *      filled rectangle
		 * @see {@link MapCanvas#draw(MapCanvas, int, int)} for drawing a
		 *      {@code MapCanvas} or {@link MapTexture}
		 * @see {@link MapCanvas#draw(MapFont, int, int, byte, CharSequence)} for
		 *      drawing text using a given {@link MapFont}
		 * @see {@link MapWidget#display} for obtaining the display that this widget is
		 *      attached to
		 * @see {@link MapDisplay#getPlugin()} for obtaining the {@code JavaPlugin} of
		 *      the display
		 */
		@Override
		public void onDraw() {
			byte borderColor = this.focused ? MapColorPalette.getColor(175, 175, 175)
					: MapColorPalette.getColor(0, 0, 0);
			byte backColor = this.mine ? MapColorPalette.getColor(170, 0, 14) : MapColorPalette.getColor(192, 192, 192);
			byte coverColor = this.pressed ? MapColorPalette.getColor(109, 109, 109)
					: MapColorPalette.getColor(145, 145, 145);

			int w = this.getWidth(), h = this.getHeight();

			// Borders
			this.view.drawRectangle(0, 0, w, h, borderColor);

			// Text and Icons
			boolean largeFont = h > 9;
			MapFont<Character> font = largeFont ? MapFont.MINECRAFT : MapFont.TINY;
			MapTexture flag = largeFont ? ((Main) this.display.getPlugin()).getFlagTextureNormal()
					: ((Main) this.display.getPlugin()).getFlagTextureTiny();
			int w_off = largeFont ? 2 : 1, h_off = largeFont ? 3 : 2;

			if (this.covered) {
				this.view.fillRectangle(1, 1, w - 2, h - 2, coverColor);

				if (this.flagged) {
					this.view.draw(flag, w / 2 - w_off, h / 2 - h_off);
				}

			} else {
				// Inside
				this.view.fillRectangle(1, 1, w - 2, h - 2, backColor);

				// Text Colors
				byte[] colors = new byte[] { MapColorPalette.getColor(255, 255, 255),
						MapColorPalette.getColor(0, 0, 255), MapColorPalette.getColor(0, 127, 0),
						MapColorPalette.getColor(255, 0, 0), MapColorPalette.getColor(0, 0, 127),
						MapColorPalette.getColor(127, 0, 0), MapColorPalette.getColor(0, 127, 127),
						MapColorPalette.getColor(127, 0, 127), MapColorPalette.getColor(127, 127, 127) };

				if (this.mine) {
					this.view.draw(font, w / 2 - w_off, h / 2 - h_off, colors[0], "X");
				} else if (this.value == 0) {
					this.view.draw(font, w / 2 - w_off, h / 2 - h_off, colors[0], "");
				} else {
					this.view.draw(font, w / 2 - w_off, h / 2 - h_off, colors[this.value], String.valueOf(this.value));
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This method is separated into 2 parts.
		 * <p>
		 * When the {@code ENTER} key or {@code BACK} key is pressed, set the
		 * pseudo-state pressed, and trigger the widget to redraw.
		 * <p>
		 * When a direction key is pressed, override the default navigation and call the
		 * custom navigation routine.
		 * <p>
		 * It is important that the key press event is sent to the {@code MapWidget}
		 * parent ({@code super}) if the event is not handled, otherwise it will not
		 * propagate.
		 *
		 * @param event the key press event that was received
		 * @see {@link #onTileNavigate(MapKeyEvent)} for more information of the custom
		 *      navigation routine
		 * @see {@link MapWidget#invalidate()} for triggering the widget to redraw
		 */
		@Override
		public void onKeyPressed(MapKeyEvent event) {
			switch (event.getKey()) {
			case ENTER:
			case BACK:
				this.pressed = true;
				this.invalidate();
				return;
			case UP:
			case DOWN:
			case LEFT:
			case RIGHT:
				// Navigation overridden as it is jumping diagonals.
				this.onTileNavigate(event);
				return;
			}

			super.onKeyPressed(event);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This method first confirms that the pseudo-state pressed is set, then it
		 * unsets the pressed state and separates into 2 parts.
		 * <p>
		 * When the {@code ENTER} key is released, uncover the tile.
		 * <p>
		 * When the {@code BACK} key is released, toggle the flagged state of the tile.
		 * <p>
		 * It is important that the key release event is sent to the {@code MapWidget}
		 * parent ({@code super}) if the event is not handled, otherwise it will not
		 * propagate.
		 *
		 * @param event the key release event that was received
		 * @see {@link #tryUncover()} for uncovering the tile
		 * @see {@link #setFlagged()} for setting the flagged state
		 */
		@Override
		public void onKeyReleased(MapKeyEvent event) {
			if (this.pressed) {
				this.pressed = false;
				if (event.getKey() == MapPlayerInput.Key.ENTER) {
					this.tryUncover();
					return;
				}
				if (event.getKey() == MapPlayerInput.Key.BACK) {
					this.trySetFlagged(!this.flagged);
					return;
				}
			}

			super.onKeyReleased(event);
		}

		/**
		 * The custom tile navigation logic
		 * <p>
		 * This method is expected to be overridden. Without overriding, by default, it
		 * delegates navigation to the default navigation logic.
		 *
		 * @param event the key event
		 * @see {@link MapWidget#handleNavigation(MapKeyEvent)} for the default
		 *      navigation logic
		 */
		public void onTileNavigate(MapKeyEvent event) {
			super.handleNavigation(event);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * When the correct mouse-related status event is received, the current widget
		 * is focusable, and the mouse location is within bounds of the widget:
		 * <p>
		 * <ul>
		 * <li>If it is a hover, focus the tile
		 * <li>If it is a left-click, focus and uncover the tile
		 * <li>If it is a right-click, focus the tile and toggle the flagged state
		 * </ul>
		 * <p>
		 * It is important that the status event is sent to the {@code MapWidget} parent
		 * ({@code super}) if the event is not handled, otherwise it will not propagate.
		 *
		 * @see {@link Main#isInBounds(MapWidget, int, int)} for more information on how
		 *      the in-bounds check is done
		 */
		@Override
		public void onStatusChanged(MapStatusEvent event) {
			if (!Arrays.asList("LEFT_CLICK", "RIGHT_CLICK", "HOVER").contains(event.getName()) || !this.isFocusable()) {
				return;
			}
			Point clicked = event.getArgument(Point.class);
			int clickedX = clicked.x, clickedY = clicked.y;

			if (!Main.isInBounds(this, clickedX, clickedY)) {
				return;
			}

			this.focus();
			if (event.getName().equals("LEFT_CLICK")) {
				this.tryUncover();
			} else if (event.getName().equals("RIGHT_CLICK")) {
				this.trySetFlagged(!this.flagged);
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We unset the pseudo-state {@link #pressed} and {@link #focused}, and trigger
		 * a redraw using {@link MapWidget#invalidate()}.
		 */
		@Override
		public void onBlur() {
			this.pressed = false;
			this.focused = false;
			this.invalidate();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We set the pseudo-state {@link #focused}, and trigger a redraw using
		 * {@link MapWidget#invalidate()}.
		 */
		@Override
		public void onFocus() {
			this.focused = true;
			this.invalidate();
		}

		/**
		 * Attempts to uncover a tile
		 * <p>
		 * If the widget is not attached to a display, it is already uncovered, or it
		 * has a flag, nothing happens.
		 * <p>
		 * Otherwise, it is set to uncovered, a sound is played, the callback function
		 * {@link #onUncover()} is called, and a redraw is triggered using
		 * {@link MapWidget#invalidate()}.
		 */
		public void tryUncover() {
			if (this.display == null || !this.covered || this.flagged) {
				return;
			}

			this.covered = false;

			if (this.mine) {
				this.display.playSound(SoundEffect.fromName("entity.generic.explode"), 1.0f, 1.0f);
			} else {
				this.display.playSound(SoundEffect.fromName("block.stone.break"), 1.0f, 1.0f);
			}

			this.onUncover();
			this.invalidate();
		}

		/**
		 * Gets whether the tile is covered or not
		 *
		 * @return if the tile is covered
		 */
		public boolean isCovered() {
			return this.covered;
		}

		/**
		 * Called when a tile is successfully uncovered
		 * <p>
		 * This method is can be overridden to handle post-tile-uncover logic.
		 */
		public void onUncover() {
		}

		/**
		 * Gets whether the tile is a mine or not
		 *
		 * @return if the tile is a mine
		 */
		public boolean isMine() {
			return this.mine;
		}

		/**
		 * Sets whether the tile is a mine or not
		 *
		 * @param mine if the tile should be a mine
		 */
		public void setMine(boolean mine) {
			this.mine = mine;
			this.invalidate();
		}

		/**
		 * Gets the numeric value shown on the tile
		 *
		 * @return the numeric value on the tile
		 */
		public int getValue() {
			return this.value;
		}

		/**
		 * Sets the numeric value shown on the tile
		 *
		 * @param value the new numeric value of the tile
		 */
		public void setValue(int value) {
			this.value = value;
			this.invalidate();
		}

		/**
		 * Gets whether the tile has a flag or not
		 *
		 * @return if the tile has a flag
		 */
		public boolean isFlagged() {
			return this.flagged;
		}

		/**
		 * Attempts to set whether the tile has a flag or not
		 * <p>
		 * If the widget is not attached to a display, or the tile is already uncovered,
		 * nothing happens.
		 * <p>
		 * Otherwise, the state is updated, a sound is played, and a redraw is triggered
		 * using {@link MapWidget#invalidate()}.
		 *
		 * @param flagged if the tile should be flagged or not
		 */
		public void trySetFlagged(boolean flagged) {
			if (this.display == null || !this.covered) {
				return;
			}

			this.flagged = flagged;
			this.display.playSound(SoundEffect.fromName(flagged ? "block.wool.place" : "block.wool.break"), 1.0f, 1.0f);
			this.invalidate();
		}

		/**
		 * Sets whether the tile has a flag or not
		 *
		 * @param flagged if the tile should have a flag
		 */
		private void setFlagged(boolean flagged) {
			if (!this.covered) {
				return;
			}

			this.flagged = flagged;
			this.invalidate();
		}
	}

	/**
	 * This is the difficulty selector dialog class for the Minesweeper game
	 * <p>
	 * This is a {@link MapWidgetWindow} class. Use this class to understand how to
	 * use {@code MapWidgetWindow}s.
	 * <p>
	 * This widget handles the the difficulty selection.
	 * <p>
	 * This class can have any visibility, as long as it is visible to the display
	 * class.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 */
	private static class MinesweeperDifficultySelectDialog extends MapWidgetWindow {
		/**
		 * The currently selected difficulty
		 */
		protected MinesweeperDifficulty selected = MinesweeperDifficulty.BEGINNER;

		/**
		 * The selector widget
		 */
		private MapWidget selector = null;
		/**
		 * The "Start Game" button widget
		 */
		private MapWidget button = null;

		/**
		 * Creates the dialog
		 */
		public MinesweeperDifficultySelectDialog() {
			this.setBounds(15, 22, 95, 58);
			this.setBackgroundColor(MapColorPalette.getColor(114, 121, 175));
			this.setDepthOffset(4);
			this.setFocusable(true);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In here, we activate the dialog window, then add the required widgets,
		 * setting up any behavior that is needed.
		 */
		@Override
		public void onAttached() {
			super.onAttached();
			this.activate();

			// Label
			this.addWidget(new MapWidgetText().setText("Select Difficulty").setBounds(7, 5, 76, 13));

			// Selector:
			// When pressed, it should cycle to the next difficulty
			this.selector = this.addWidget(new MapWidgetButton() {
				@Override
				public void onActivate() {
					MinesweeperDifficultySelectDialog.this.cycleNext();
					this.setText(MinesweeperDifficultySelectDialog.this.selected.name());
				}
			}.setText(this.selected.name()).setBounds(10, 25, 72, 13));

			// Start:
			// When pressed, it should signal to start the game
			this.button = this.addWidget(new MapWidgetButton() {
				@Override
				public void onActivate() {
					MinesweeperDifficultySelectDialog.this.close();
					MinesweeperDifficultySelectDialog.this.submit();
				}
			}.setText("Start Game").setBounds(10, 40, 72, 13));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * If the {@code BACK} key is pressed at any time while the dialog is activated,
		 * close it.
		 * <p>
		 * It is important that the key press event is sent to the {@code MapWidget}
		 * parent ({@code super}) if the event is not handled, otherwise it will not
		 * propagate.
		 *
		 * @param event the key press event that was received
		 */
		@Override
		public void onKeyPressed(MapKeyEvent event) {
			if (event.getKey() == MapPlayerInput.Key.BACK && this.isActivated()) {
				this.close();
				return;
			}
			super.onKeyPressed(event);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * When the correct mouse-related status event is received, the current widget
		 * is activated, and the mouse location is within bounds of the widget:
		 * <p>
		 * <ul>
		 * <li>If it is a hover, focus the hovered child widget
		 * <li>If it is a left-click, focus and activate the child widget
		 * </ul>
		 * <p>
		 * It is important that the status event is sent to the {@code MapWidget} parent
		 * ({@code super}) if the event is not handled, otherwise it will not propagate.
		 *
		 * @see {@link Main#isInBounds(MapWidget, int, int)} for more information on how
		 *      the in-bounds check is done
		 */
		@Override
		public void onStatusChanged(MapStatusEvent event) {
			if (!this.isActivated() || !Arrays.asList("LEFT_CLICK", "RIGHT_CLICK", "HOVER").contains(event.getName())) {
				return;
			}
			Point clicked = event.getArgument(Point.class);
			int clickedX = clicked.x, clickedY = clicked.y;

			if (!Main.isInBounds(this, clickedX, clickedY)) {
				return;
			}

			if (event.getName().equals("HOVER")) {
				if (Main.isInBounds(this.selector, clickedX, clickedY)) {
					this.selector.focus();
				} else if (Main.isInBounds(this.button, clickedX, clickedY)) {
					this.button.focus();
				}
			} else if (event.getName().equals("LEFT_CLICK")) {
				if (Main.isInBounds(this.selector, clickedX, clickedY)) {
					this.selector.focus();
					this.selector.activate();
				} else if (Main.isInBounds(this.button, clickedX, clickedY)) {
					this.button.focus();
					this.button.activate();
				}
			}
		}

		/**
		 * Cycles to the next difficulty setting
		 */
		private void cycleNext() {
			switch (this.selected) {
			case BEGINNER:
				this.selected = MinesweeperDifficulty.INTERMEDIATE;
				break;
			case INTERMEDIATE:
				this.selected = MinesweeperDifficulty.EXPERT;
				break;
			case EXPERT:
				this.selected = MinesweeperDifficulty.BEGINNER;
				break;
			}
		}

		/**
		 * Closes the widget
		 */
		public void close() {
			this.removeWidget();
		}

		/**
		 * Sends a trigger to the parent to capture the dialog information
		 */
		protected void submit() {
			this.onSubmit();
		}

		/**
		 * Callback for triggering the parent to capture dialog information
		 * <p>
		 * This method should be overridden to handle the submit action.
		 */
		public void onSubmit() {
		}
	}

	/**
	 * This is the game end dialog class for the Minesweeper game
	 * <p>
	 * This is a {@link MapWidgetWindow} class. Use this class to understand how to
	 * use {@code MapWidgetWindow}s.
	 * <p>
	 * This widget shows when the game has ended.
	 * <p>
	 * This class can have any visibility, as long as it is visible to the display
	 * class.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 */
	private static class MinesweeperWinLoseDialog extends MapWidgetWindow {
		/**
		 * Whether the game ended in a win or not
		 */
		private final boolean win;
		/**
		 * The time from start to end
		 */
		private final Duration time;
		/**
		 * The number of flags placed on the board
		 */
		private final int flags;
		/**
		 * The number of mines on the board
		 */
		private final int mines;

		/**
		 * The "Back to Title" button widget
		 */
		private MapWidget button = null;

		/**
		 * Creates the dialog
		 *
		 * @param win   whether the game ended in a win or not
		 * @param time  the time from start to end
		 * @param flags the number of flags placed on the board
		 * @param mines the number of mines on the board
		 */
		public MinesweeperWinLoseDialog(boolean win, Duration time, int flags, int mines) {
			this.win = win;
			this.time = time;
			this.flags = flags;
			this.mines = mines;

			this.setBounds(15, 22, 95, 58);
			this.setBackgroundColor(MapColorPalette.getColor(114, 121, 175));
			this.setDepthOffset(4);
			this.setFocusable(true);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In here, we activate the dialog window, then add the required widgets,
		 * setting up any behavior that is needed.
		 */
		@Override
		public void onAttached() {
			super.onAttached();
			this.activate();

			// Label
			this.addWidget(new MapWidgetText().setText(this.win ? "You Win!" : "You Lose").setBounds(5, 5, 80, 13));

			// Time
			this.addWidget(new MapWidgetText()
					.setText(String.format("Time: %02d:%02d", this.time.toMinutes(), this.time.toMillis() / 1000 % 60))
					.setFont(MapFont.TINY).setBounds(5, 17, 80, 13));

			// Mines
			this.addWidget(new MapWidgetText().setText(String.format("Flags placed: %d/%d", this.flags, this.mines))
					.setFont(MapFont.TINY).setBounds(5, 24, 80, 13));

			// Button
			this.button = this.addWidget(new MapWidgetButton() {
				@Override
				public void onActivate() {
					MinesweeperWinLoseDialog.this.close();
				}
			}.setText("Back to Title").setBounds(10, 40, 72, 13));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * If the {@code BACK} key is pressed at any time while the dialog is activated,
		 * close it.
		 * <p>
		 * It is important that the key press event is sent to the {@code MapWidget}
		 * parent ({@code super}) if the event is not handled, otherwise it will not
		 * propagate.
		 *
		 * @param event the key press event that was received
		 */
		@Override
		public void onKeyPressed(MapKeyEvent event) {
			if (event.getKey() == MapPlayerInput.Key.BACK && this.isActivated()) {
				this.close();
				return;
			}
			super.onKeyPressed(event);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * When the correct mouse-related status event is received, the current widget
		 * is activated, and the mouse location is within bounds of the widget:
		 * <p>
		 * <ul>
		 * <li>If it is a hover, focus the hovered child widget
		 * <li>If it is a left-click, focus and activate the child widget
		 * </ul>
		 * <p>
		 * It is important that the status event is sent to the {@code MapWidget} parent
		 * ({@code super}) if the event is not handled, otherwise it will not propagate.
		 *
		 * @see {@link Main#isInBounds(MapWidget, int, int)} for more information on how
		 *      the in-bounds check is done
		 */
		@Override
		public void onStatusChanged(MapStatusEvent event) {
			if (!this.isActivated() || !Arrays.asList("LEFT_CLICK", "RIGHT_CLICK", "HOVER").contains(event.getName())) {
				return;
			}
			Point clicked = event.getArgument(Point.class);
			int clickedX = clicked.x, clickedY = clicked.y;

			if (!Main.isInBounds(this, clickedX, clickedY)) {
				return;
			}

			if (event.getName().equals("HOVER")) {
				if (Main.isInBounds(this.button, clickedX, clickedY)) {
					this.button.focus();
				}
			} else if (event.getName().equals("LEFT_CLICK")) {
				if (Main.isInBounds(this.button, clickedX, clickedY)) {
					this.button.focus();
					this.button.activate();
				}
			}
		}

		/**
		 * Closes the dialog
		 */
		public void close() {
			this.removeWidget();
			this.onClose();
		}

		/**
		 * Callback after the dialog has been closed
		 * <p>
		 * This method should be overridden to handle the post-close action.
		 */
		public void onClose() {
		}
	}

	/**
	 * This represents the game state of the Minesweeper game
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 */
	private enum GameState {
		/**
		 * The state while on the title
		 */
		TITLE,
		/**
		 * The state while in-game
		 */
		GAME,
		/**
		 * The state while the game-ended in a win
		 */
		WIN,
		/**
		 * The state while the game-ended in a lose
		 */
		LOSE
	}

	/**
	 * This represents the difficulty of the Minesweeper game
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 */
	private enum MinesweeperDifficulty {
		/**
		 * The easy difficulty
		 */
		BEGINNER(9, 9, 10),
		/**
		 * The normal difficulty
		 */
		INTERMEDIATE(14, 14, 30),
		/**
		 * The hard difficulty
		 */
		EXPERT(18, 18, 69);

		/**
		 * Number of rows in the difficulty
		 */
		private final int rows;
		/**
		 * Number of columns in the difficulty
		 */
		private final int cols;
		/**
		 * Number of mines in the difficulty
		 */
		private final int mines;

		/**
		 * Creates a difficulty
		 *
		 * @param rows  the number of rows
		 * @param cols  the number of columns
		 * @param mines the number of mines
		 */
		private MinesweeperDifficulty(int rows, int cols, int mines) {
			this.rows = rows;
			this.cols = cols;
			this.mines = mines;
		}

		/**
		 * Gets the number of rows in the difficulty
		 *
		 * @return the number of rows
		 */
		public int rows() {
			return this.rows;
		}

		/**
		 * Gets the number of columns in the difficulty
		 *
		 * @return the number of columns
		 */
		public int cols() {
			return this.cols;
		}

		/**
		 * Gets the number of mines in the difficulty
		 *
		 * @return the number of mines
		 */
		public int mines() {
			return this.mines;
		}
	}
}
