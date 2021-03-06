package binnie.core.machines.storage;

import binnie.core.BinnieCore;
import binnie.core.api.gui.Alignment;
import binnie.core.api.gui.ITexture;
import binnie.core.api.gui.events.EventHandlerOrigin;
import binnie.core.gui.Attribute;
import binnie.core.gui.CraftGUI;
import binnie.core.gui.controls.ControlText;
import binnie.core.gui.controls.ControlTextEdit;
import binnie.core.gui.controls.button.ControlButton;
import binnie.core.gui.controls.core.Control;
import binnie.core.gui.controls.page.ControlPage;
import binnie.core.gui.controls.page.ControlPages;
import binnie.core.gui.controls.tab.ControlTabBar;
import binnie.core.gui.events.EventMouse;
import binnie.core.gui.events.EventTextEdit;
import binnie.core.gui.events.EventValueChanged;
import binnie.core.gui.geometry.CraftGUIUtil;
import binnie.core.gui.geometry.Point;
import binnie.core.gui.minecraft.EnumColor;
import binnie.core.gui.minecraft.IWindowAffectsShiftClick;
import binnie.core.gui.minecraft.MinecraftGUI;
import binnie.core.gui.minecraft.Window;
import binnie.core.gui.minecraft.control.ControlItemDisplay;
import binnie.core.gui.minecraft.control.ControlPlayerInventory;
import binnie.core.gui.minecraft.control.ControlSlide;
import binnie.core.gui.minecraft.control.ControlSlotArray;
import binnie.core.gui.minecraft.control.ControlTabIcon;
import binnie.core.gui.renderer.RenderUtil;
import binnie.core.gui.resource.textures.CraftGUITexture;
import binnie.core.gui.window.Panel;
import binnie.core.gui.window.WindowMachine;
import binnie.core.machines.IMachine;
import binnie.core.machines.Machine;
import binnie.core.machines.MachinePackage;
import binnie.core.machines.transfer.TransferRequest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WindowCompartment extends WindowMachine implements IWindowAffectsShiftClick {
	private final Map<Panel, Integer> panels;
	private ControlTextEdit tabName;
	private ControlItemDisplay tabIcon;
	private ControlColourSelector tabColour;
	private int currentTab;

	public WindowCompartment(final EntityPlayer player, @Nullable final IInventory inventory, final Side side) {
		super(320, 226, player, inventory, side);
		this.panels = new HashMap<>();
		this.currentTab = 0;
	}

	//TODO: Clean Up, Localise
	@Override
	@SideOnly(Side.CLIENT)
	public void initialiseClient() {
		IInventory inventory = this.getInventory();
		IMachine machine = Machine.getMachine(inventory);
		MachinePackage machinePackage = machine.getPackage();
		this.setTitle(machinePackage.getDisplayName());
		int x = 16;
		final int y = 32;
		final ComponentCompartmentInventory inv = machine.getInterface(ComponentCompartmentInventory.class);
		Integer[] tabs1 = new Integer[0];
		Integer[] tabs2 = new Integer[0];
		if (inv.getTabCount() == 4) {
			tabs1 = new Integer[]{0, 1};
			tabs2 = new Integer[]{2, 3};
		}
		if (inv.getTabCount() == 6) {
			tabs1 = new Integer[]{0, 1, 2};
			tabs2 = new Integer[]{3, 4, 5};
		}
		if (inv.getTabCount() == 8) {
			tabs1 = new Integer[]{0, 1, 2, 3};
			tabs2 = new Integer[]{4, 5, 6, 7};
		}
		final boolean doubleTabbed = tabs2.length > 0;
		final int compartmentPageWidth = 16 + 18 * inv.getPageSize() / 5;
		final int compartmentPageHeight = 106;
		final int compartmentWidth = compartmentPageWidth + (doubleTabbed ? 48 : 24);
		final int compartmentHeight = compartmentPageHeight;
		final Control controlCompartment = new Control(this, x, y, compartmentWidth, compartmentHeight);
		final ControlTabBar<Integer> tab = new ControlTabBar<>(controlCompartment, 0, 0, 24, compartmentPageHeight, Alignment.LEFT, Arrays.asList(tabs1), (x1, y1, w, h, value) -> {
			return new CompartmentTabIcon(this, x1, y1, w, h, value);
		});
		final String[] tabHelp = {"Compartment Tab", "Tabs that divide the inventory into sections. Each one can be labelled seperately."};
		tab.addHelp(tabHelp);
		tab.addEventHandler(EventValueChanged.class, EventHandlerOrigin.DIRECT_CHILD, tab, event -> {
			if (event.getValue() == null) {
				return;
			}
			final NBTTagCompound nbt = new NBTTagCompound();
			final int i = (Integer) event.getValue();
			nbt.setByte("i", (byte) i);
			Window.get(tab).sendClientAction("tab-change", nbt);
			WindowCompartment.this.currentTab = i;
		});
		x += 24;
		final ControlPages<Integer> compartmentPages = new ControlPages<>(controlCompartment, 24, 0, compartmentPageWidth, compartmentPageHeight);
		final ControlPage[] page = new ControlPage[inv.getTabCount()];
		for (int p = 0; p < inv.getTabCount(); ++p) {
			page[p] = new ControlPage<>(compartmentPages, p);
		}
		CraftGUIUtil.linkWidgets(tab, compartmentPages);
		int i = 0;
		for (int p2 = 0; p2 < inv.getTabCount(); ++p2) {
			final ControlPage thisPage = page[p2];
			final Panel panel = new CompartmentCenterPanel(this, thisPage);
			this.panels.put(panel, p2);
			final int[] slotsIDs = new int[inv.getPageSize()];
			for (int k = 0; k < inv.getPageSize(); ++k) {
				slotsIDs[k] = i++;
			}
			new ControlSlotArray.Builder(thisPage, 8, 8, inv.getPageSize() / 5, 5).create(slotsIDs);
		}
		x += compartmentPageWidth;
		if (tabs2.length > 0) {
			final ControlTabBar<Integer> tab2 = new ControlTabBar<>(controlCompartment, 24 + compartmentPageWidth, 0, 24, compartmentPageHeight, Alignment.RIGHT, Arrays.asList(tabs2), (x1, y1, w, h, value) -> {
				return new CompartmentTabIcon(this, x1, y1, w, h, value);
			});
			tab2.setValue(tabs1[0]);
			tab2.addHelp(tabHelp);
			tab2.addEventHandler(EventValueChanged.class, EventHandlerOrigin.DIRECT_CHILD, tab2, event -> {
				if (event.getValue() == null) {
					return;
				}
				final NBTTagCompound nbt = new NBTTagCompound();
				final int iVal = (Integer) event.getValue();
				nbt.setByte("i", (byte) iVal);
				Window.get(tab).sendClientAction("tab-change", nbt);
				WindowCompartment.this.currentTab = iVal;
			});
			CraftGUIUtil.linkWidgets(tab2, compartmentPages);
			x += 24;
		}
		x += 16;
		this.setSize(new Point(Math.max(32 + compartmentWidth, 252), this.getHeight()));
		controlCompartment.setPosition(new Point((this.getWidth() - controlCompartment.getWidth()) / 2, controlCompartment.getYPos()));
		final ControlPlayerInventory invent = new ControlPlayerInventory(this, true);
		final ControlSlide slide = new ControlSlide(this, 0, 134, 136, 92, Alignment.LEFT);
		slide.setLabel("Tab Properties");
		slide.setSlide(false);
		slide.addHelp("Tab Properties");
		slide.addHelp("The label, colour and icon of the Tab can be altered here. Clicking on the icon with a held item will change it.");
		final Panel tabPropertyPanel = new Panel(slide, 16, 8, 112, 76, MinecraftGUI.PanelType.GRAY);
		int y2 = 4;
		new ControlText(tabPropertyPanel, new Point(4, y2), "Tab Name:");
		final Panel parent = tabPropertyPanel;
		final int x2 = 4;
		y2 += 12;
		(this.tabName = new ControlTextEdit(parent, x2, y2, 104, 12)).addEventHandler(EventTextEdit.class, EventHandlerOrigin.SELF, this.tabName, event -> {
			final binnie.core.machines.storage.CompartmentTab currentTab = WindowCompartment.this.getCurrentTab();
			currentTab.setName(event.getValue());
			final NBTTagCompound nbt = new NBTTagCompound();
			currentTab.writeToNBT(nbt);
			WindowCompartment.this.sendClientAction("comp-change-tab", nbt);
		});
		y2 += 20;
		new ControlText(tabPropertyPanel, new Point(4, y2), "Tab Icon: ");
		(this.tabIcon = new ControlItemDisplay(tabPropertyPanel, 58, y2 - 4)).setItemStack(new ItemStack(Items.PAPER));
		this.tabIcon.addAttribute(Attribute.MOUSE_OVER);
		this.tabIcon.addSelfEventHandler(EventMouse.Down.class, event -> {
			if (WindowCompartment.this.getHeldItemStack().isEmpty()) {
				return;
			}
			final binnie.core.machines.storage.CompartmentTab currentTab = WindowCompartment.this.getCurrentTab();
			final ItemStack stack = WindowCompartment.this.getHeldItemStack().copy();
			stack.setCount(1);
			currentTab.setIcon(stack);
			final NBTTagCompound nbt = new NBTTagCompound();
			currentTab.writeToNBT(nbt);
			WindowCompartment.this.sendClientAction("comp-change-tab", nbt);
		});
		this.tabColour = new ControlColourSelector(tabPropertyPanel, 82, y2 - 4, 16, 16, EnumColor.WHITE);
		this.tabIcon.addHelp("Icon for Current Tab");
		this.tabIcon.addHelp("Click here with an item to change");
		y2 += 20;
		new ControlText(tabPropertyPanel, new Point(4, y2), "Colour: ");
		final int cw = 8;
		final Panel panelColour = new Panel(tabPropertyPanel, 40, y2 - 4, cw * 8 + 2, cw * 2 + 1, MinecraftGUI.PanelType.GRAY);
		for (int cc = 0; cc < 16; ++cc) {
			final ControlColourSelector color = new ControlColourSelector(panelColour, 1 + cw * (cc % 8), 1 + cw * (cc / 8), cw, cw, EnumColor.values()[cc]);
			color.addSelfEventHandler(EventMouse.Down.class, event -> {
				final binnie.core.machines.storage.CompartmentTab currentTab = WindowCompartment.this.getCurrentTab();
				currentTab.setColor(color.getValue());
				final NBTTagCompound nbt = new NBTTagCompound();
				currentTab.writeToNBT(nbt);
				WindowCompartment.this.sendClientAction("comp-change-tab", nbt);
			});
			color.addHelp("Colour Selector");
			color.addHelp("Select a colour to highlight the current tab");
		}
		y2 += 20;
		final ControlButton searchButton = new SearchButton(this, controlCompartment, compartmentWidth, compartmentPageHeight);
		searchButton.addHelp("Search Button");
		searchButton.addHelp("Clicking this will open the Search dialog. This allows you to search the inventory for specific items.");
	}

	@SideOnly(Side.CLIENT)
	public void createSearchDialog() {
		new SearchDialog(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onUpdateClient() {
		super.onUpdateClient();
		this.updateTabs();
	}

	@SideOnly(Side.CLIENT)
	public void updateTabs() {
		binnie.core.machines.storage.CompartmentTab currentTab = this.getCurrentTab();
		this.tabName.setValue(currentTab.getName());
		this.tabIcon.setItemStack(currentTab.getIcon());
		this.tabColour.setValue(currentTab.getColor());
	}

	@Override
	public void receiveGuiNBTOnServer(final EntityPlayer player, final String name, final NBTTagCompound nbt) {
		super.receiveGuiNBTOnServer(player, name, nbt);
		if (name.equals("tab-change")) {
			this.currentTab = nbt.getByte("i");
		}
	}

	@Override
	public String getTitle() {
		return "Compartment";
	}

	@Override
	protected String getModId() {
		return BinnieCore.getInstance().getModId();
	}

	@Override
	protected String getBackgroundTextureName() {
		return "compartment";
	}

	@Override
	public void alterRequest(final TransferRequest request) {
		if (request.getDestination() == this.getInventory()) {
			final ComponentCompartmentInventory inv = Machine.getMachine(this.getInventory()).getInterface(ComponentCompartmentInventory.class);
			request.setTargetSlots(inv.getSlotsForTab(this.currentTab));
		}
	}

	public CompartmentTab getTab(final int i) {
		return Machine.getInterface(ComponentCompartmentInventory.class, this.getInventory()).getTab(i);
	}

	public CompartmentTab getCurrentTab() {
		return this.getTab(this.currentTab);
	}

	private static class CompartmentTabIcon extends ControlTabIcon<Integer> {
		private final WindowCompartment windowCompartment;

		public CompartmentTabIcon(WindowCompartment windowCompartment, int x, int y, int w, int h, Integer value) {
			super(x, y, w, h, value);
			this.windowCompartment = windowCompartment;
		}

		@Override
		public ItemStack getItemStack() {
			binnie.core.machines.storage.CompartmentTab tab = windowCompartment.getTab(this.value);
			return tab.getIcon();
		}

		@Override
		public String getName() {
			return windowCompartment.getTab(this.value).getName();
		}

		@Override
		public int getOutlineColour() {
			return windowCompartment.getTab(this.value).getColor().getColor();
		}

		@Override
		public boolean hasOutline() {
			return true;
		}
	}

	private static class CompartmentCenterPanel extends Panel {
		private final WindowCompartment windowCompartment;

		public CompartmentCenterPanel(WindowCompartment windowCompartment, ControlPage thisPage) {
			super(thisPage, 0, 0, thisPage.getWidth(), thisPage.getHeight(), MinecraftGUI.PanelType.BLACK);
			this.windowCompartment = windowCompartment;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void onRenderForeground(int guiWidth, int guiHeight) {
			final ITexture iTexture = CraftGUI.RENDER.getTexture(CraftGUITexture.TAB_OUTLINE);
			binnie.core.machines.storage.CompartmentTab tab = windowCompartment.getTab(windowCompartment.panels.get(this));
			RenderUtil.setColour(tab.getColor().getColor());
			CraftGUI.RENDER.texture(iTexture, this.getArea().inset(3));
		}
	}

	private static class SearchButton extends ControlButton {
		private final WindowCompartment windowCompartment;

		public SearchButton(WindowCompartment windowCompartment, Control controlCompartment, int compartmentWidth, int compartmentPageHeight) {
			super(controlCompartment, compartmentWidth - 24 - 64 - 8, compartmentPageHeight, 64, 16, "Search");
			this.windowCompartment = windowCompartment;
		}

		@Override
		@SideOnly(Side.CLIENT)
		protected void onMouseClick(final EventMouse.Down event) {
			windowCompartment.createSearchDialog();
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void onRenderBackground(int guiWidth, int guiHeight) {
			final Object texture = this.isMouseOver() ? CraftGUITexture.TAB_HIGHLIGHTED : CraftGUITexture.TAB;
			CraftGUI.RENDER.texture(CraftGUI.RENDER.getTexture(texture).crop(Alignment.BOTTOM, 8), this.getArea());
		}
	}
}
