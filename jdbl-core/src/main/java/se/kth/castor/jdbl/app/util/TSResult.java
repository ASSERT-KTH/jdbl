package se.kth.castor.jdbl.app.util;

public class TSResult
{
   private final int totalTests;
   private final int failedTests;
   private final int errorTests;
   private final int skippedTests;

   public TSResult()
   {
      totalTests = 0;
      failedTests = 0;
      errorTests = 0;
      skippedTests = 0;
   }

   public TSResult(final int totalTests, final int failedTests, final int errorTests, final int skippedTests)
   {
      this.totalTests = totalTests;
      this.failedTests = failedTests;
      this.errorTests = errorTests;
      this.skippedTests = skippedTests;
   }

   public String getResults()
   {
      return "Tests run: " + totalTests() + ", Failures: " + failedTests() +
      ", Errors: " + errorTests() + ", Skipped: " + skippedTests() + "\n";
   }

   public int totalTests()
   {
      return totalTests;
   }

   public int failedTests()
   {
      return failedTests;
   }

   public int errorTests()
   {
      return errorTests;
   }

   public int skippedTests()
   {
      return skippedTests;
   }

   @Override
   public String toString() {
      return getResults();
   }
}
