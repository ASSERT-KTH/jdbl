package se.kth.castor.jdbl.app;

import java.util.Arrays;

/**
 * The debloat strategies supported by this plugin.
 */
public enum DebloatTypeEnum
{
   TEST_DEBLOAT,
   ENTRY_POINT_DEBLOAT,
   CONSERVATIVE_DEBLOAT;

   @Override
   public String toString()
   {
      return "Debloat strategies: " + Arrays.toString(DebloatTypeEnum.values());
   }
}
