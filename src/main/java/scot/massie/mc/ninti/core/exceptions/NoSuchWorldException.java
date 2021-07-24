package scot.massie.mc.ninti.core.exceptions;

/**
 * Thrown when a world is referenced by ID in such a way that a world instance is required, but no world exists by the
 * given ID.
 */
public class NoSuchWorldException extends RuntimeException
{
    /**
     * Creates a new NoSuchWorldException.
     * @param worldId The world ID which didn't match any existing world.
     */
    public NoSuchWorldException(String worldId)
    {
        super("No world with the ID: " + worldId);
        this.worldId = worldId;
    }

    protected final String worldId;

    /**
     * Gets the world ID that doesn't match any existing world.
     * @return The world ID.
     */
    public String getWorldId()
    { return worldId; }
}
