package world.bentobox.likes.panels.util;


import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.function.BiConsumer;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.likes.panels.GuiUtils;
import world.bentobox.likes.utils.Constants;


/**
 * This class contains all necessary things that allows to select single block from all ingame blocks. Selected
 * block will be returned via BiConsumer.
 */
public class SelectBlocksGUI
{
	private SelectBlocksGUI(User user, boolean singleSelect, Set<Material> excludedMaterial, BiConsumer<Boolean, Set<Material>> consumer)
	{
		this.consumer = consumer;
		this.user = user;
		this.singleSelect = singleSelect;

		// Current GUI cannot display air blocks. It crashes with null-pointer
		excludedMaterial.add(Material.AIR);
		excludedMaterial.add(Material.CAVE_AIR);
		excludedMaterial.add(Material.VOID_AIR);

		// Piston head and moving piston is not necessary. useless.
		excludedMaterial.add(Material.PISTON_HEAD);
		excludedMaterial.add(Material.MOVING_PISTON);

		// Barrier cannot be accessible to user.
		excludedMaterial.add(Material.BARRIER);

		this.elements = new ArrayList<>();
		this.selectedMaterials = new HashSet<>();

		for (Material material : Material.values())
		{
			if (material.isBlock() && !material.isLegacy() && !excludedMaterial.contains(material))
			{
				this.elements.add(material);
			}
		}
	}


	public static void open(User user, BiConsumer<Boolean, Set<Material>> consumer)
	{
		new SelectBlocksGUI(user, true, new HashSet<>(), consumer).build(0);
	}


// ---------------------------------------------------------------------
// Section: Methods
// ---------------------------------------------------------------------


	/**
	 * This method builds all necessary elements in GUI panel.
	 */
	private void build(int pageIndex)
	{
		PanelBuilder panelBuilder = new PanelBuilder().user(this.user).
			name(this.user.getTranslation(Constants.TITLE + "select-block"));

		GuiUtils.fillBorder(panelBuilder, Material.BLUE_STAINED_GLASS_PANE);

		final int MAX_ELEMENTS = 21;
		final int correctPage;

		if (pageIndex < 0)
		{
			correctPage = this.elements.size() / MAX_ELEMENTS;
		}
		else if (pageIndex > (this.elements.size() / MAX_ELEMENTS))
		{
			correctPage = 0;
		}
		else
		{
			correctPage = pageIndex;
		}

		int entitiesIndex = MAX_ELEMENTS * correctPage;

		// I want first row to be only for navigation and return button.
		int index = 10;

		while (entitiesIndex < ((correctPage + 1) * MAX_ELEMENTS) &&
			entitiesIndex < this.elements.size())
		{
			if (!panelBuilder.slotOccupied(index))
			{
				panelBuilder.item(index, this.createMaterialButton(this.elements.get(entitiesIndex++)));
			}

			index++;
		}

		panelBuilder.item(3,
			new PanelItemBuilder().
				icon(Material.RED_STAINED_GLASS_PANE).
				name(this.user.getTranslation(Constants.BUTTON + "cancel")).
				clickHandler( (panel, user1, clickType, slot) -> {
					this.consumer.accept(false, null);
					return true;
				}).build());


		List<String> description = new ArrayList<>();
		if (!this.selectedMaterials.isEmpty())
		{
			description.add(this.user.getTranslation(Constants.DESCRIPTION + "selected") + ":");
			this.selectedMaterials.forEach(material -> description.add(" - " + material.name()));
		}

		panelBuilder.item(5,
			new PanelItemBuilder().
				icon(Material.GREEN_STAINED_GLASS_PANE).
				name(this.user.getTranslation(Constants.BUTTON + "accept")).
				description(description).
				clickHandler( (panel, user1, clickType, slot) -> {
					this.consumer.accept(true, this.selectedMaterials);
					return true;
				}).build());

		if (this.elements.size() > MAX_ELEMENTS)
		{
			// Navigation buttons if necessary

			panelBuilder.item(18,
				new PanelItemBuilder().
					icon(Material.OAK_SIGN).
					name(this.user.getTranslation(Constants.BUTTON + "previous")).
					clickHandler((panel, user1, clickType, slot) -> {
						this.build(correctPage - 1);
						return true;
					}).build());

			panelBuilder.item(26,
				new PanelItemBuilder().
					icon(Material.OAK_SIGN).
					name(this.user.getTranslation(Constants.BUTTON + "next")).
					clickHandler((panel, user1, clickType, slot) -> {
						this.build(correctPage + 1);
						return true;
					}).build());
		}

		panelBuilder.item(44,
			new PanelItemBuilder().
				icon(Material.OAK_DOOR).
				name(this.user.getTranslation(Constants.BUTTON + "return")).
				clickHandler( (panel, user1, clickType, slot) -> {
					this.consumer.accept(false, null);
					return true;
				}).build());

		panelBuilder.build();
	}


	/**
	 * This method creates PanelItem that represents given material.
	 * Some materials is not displayable in Inventory GUI, so they are replaced with "placeholder" items.
	 * @param material Material which icon must be created.
	 * @return PanelItem that represents given material.
	 */
	private PanelItem createMaterialButton(Material material)
	{
		ItemStack itemStack = GuiUtils.getMaterialItem(material);

		return new PanelItemBuilder().
			name(WordUtils.capitalize(material.name().toLowerCase().replace("_", " "))).
			description(this.selectedMaterials.contains(material) ?
				this.user.getTranslation(Constants.DESCRIPTION + "selected") : "").
			icon(itemStack).
			clickHandler((panel, user1, clickType, slot) -> {
				if (!this.singleSelect && clickType.isRightClick())
				{
					if (!this.selectedMaterials.add(material))
					{
						this.selectedMaterials.remove(material);
					}

					panel.getInventory().setItem(slot, this.createMaterialButton(material).getItem());
				}
				else
				{
					this.selectedMaterials.add(material);
					this.consumer.accept(true, this.selectedMaterials);
				}

				return true;
			}).
			glow(!itemStack.getType().equals(material)).
			build();
	}


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------

	/**
	 * List with elements that will be displayed in current GUI.
	 */
	private List<Material> elements;

	/**
	 * Set that contains selected materials.
	 */
	private Set<Material> selectedMaterials;

	/**
	 * This variable stores consumer.
	 */
	private BiConsumer<Boolean, Set<Material>> consumer;

	/**
	 * User who runs GUI.
	 */
	private User user;

	/**
	 * This indicate that return set must contain only single item.
	 */
	private boolean singleSelect;
}
