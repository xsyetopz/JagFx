using System.Reflection;

namespace JagFx.TestData;

/// <summary>
/// Provides access to embedded test resource files.
/// </summary>
public static class TestResources
{
    private static readonly Assembly Assembly = typeof(TestResources).Assembly;
    private const string ResourcePrefix = "JagFx.TestData.Resources";

    /// <summary>
    /// Gets the raw hex string content of an embedded resource.
    /// </summary>
    /// <param name="name">The resource name (e.g., "cow_death").</param>
    /// <returns>The hex string content.</returns>
    public static string GetHexString(string name)
    {
        var resourceName = $"{ResourcePrefix}.{name}.hex";
        using var stream = Assembly.GetManifestResourceStream(resourceName)
            ?? throw new FileNotFoundException($"Resource not found: {resourceName}");
        using var reader = new StreamReader(stream);
        return reader.ReadToEnd().Trim();
    }

    /// <summary>
    /// Gets the binary bytes from an embedded hex resource.
    /// </summary>
    /// <param name="name">The resource name (e.g., "cow_death").</param>
    /// <returns>The decoded byte array.</returns>
    public static byte[] GetBytes(string name)
    {
        var hex = GetHexString(name);
        return HexToBytes(hex);
    }

    /// <summary>
    /// Converts a hex string to a byte array.
    /// </summary>
    private static byte[] HexToBytes(string hex)
    {
        hex = hex.Replace(" ", "").Replace("\n", "").Replace("\r", "");
        var bytes = new byte[hex.Length / 2];
        for (var i = 0; i < bytes.Length; i++)
        {
            bytes[i] = Convert.ToByte(hex.Substring(i * 2, 2), 16);
        }
        return bytes;
    }

    #region Convenience Properties

    public static byte[] CowDeath => GetBytes("cow_death");
    public static byte[] NoaMeleeAttackMovement => GetBytes("noa_melee_attack_movement");
    public static byte[] NoaMeleeScratchFloor7 => GetBytes("noa_melee_scratch_floor_7");
    public static byte[] NoaScreechMelee3 => GetBytes("noa_screech_melee_3");
    public static byte[] RelicUnlockPulsing => GetBytes("relic_unlock_pulsing");
    public static byte[] RelicUnlockSpinningHover => GetBytes("relic_unlock_spinning_hover");
    public static byte[] SpawnBoneCrack2 => GetBytes("spawn_bone_crack_2");
    public static byte[] ToaZebakAttackJawShut02 => GetBytes("toa_zebak_attack_jaw_shut_02");
    public static byte[] ToaZebakAttackMeleeEnragedJawSnap01 => GetBytes("toa_zebak_attack_melee_enranged_jaw_snap_01");
    public static byte[] ToaZebakAttackMeleeHighRoar02 => GetBytes("toa_zebak_attack_melee_high_roar_02");
    public static byte[] ToaZebakAttackMeleeJawSnapShut01 => GetBytes("toa_zebak_attack_melee_jaw_snap_shut_01");
    public static byte[] ToaZebakAttackMeleeRoar01 => GetBytes("toa_zebak_attack_melee_roar_01");
    public static byte[] ToaZebakAttackRoar05 => GetBytes("toa_zebak_attack_roar_05");
    public static byte[] ToaZebakRangedEnragedGulp02 => GetBytes("toa_zebak_ranged_enranged_gulp_02");
    public static byte[] WardOfArceuusCast => GetBytes("ward_of_arceuus_cast");

    #endregion
}
