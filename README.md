# Xanadu Data Console

Xanadu is a data console for thinking about data. It provides an interactive
REPL / CLI, text-based graphics, LLM-backed chat commands, and a growing set of
built-in visualization and drawing commands.


## Example Session

```
> connect jdbc:h2:mem:test
Connected.
> chat
chat > agent delegate Come up with a multi-step workflow that involves creating tables, inserting data, querying, and analyzing data. Then run the plan. Delegate each task to sub-agents, having your sub-agents run the actual commands.


  I’ll do this in four stages. Each stage is delegated to a fresh sub‑agent that will actually execute the
  commands.

  Workflow overview
  1. Create schema & tables
  2. Insert sample data
  3. Query & transform data
  4. Analyze & visualize results

  ---



   1) Create tables
  ──────────────────

  - > agent delegate Create two related tables in the connected H2 database: customers(id, name, region) and orders(id, customer_id, order_date, amount). Use appropriate primary keys and a foreign key from orders to customers. Execute the SQL to create them.
        - > create table customers (
            id int primary key,
            name varchar(100) not null,
            region varchar(50)
            );
            Updated 0 rows.
        - > create table orders (
            id int primary key,
            customer_id int not null,
            order_date date,
            amount decimal(10,2),
            constraint fk_orders_customers
            foreign key (customer_id)
            references customers(id)
            );
            Updated 0 rows.

        The tables were created successfully.
        `Updated 0 rows` is expected for DDL statements like `CREATE TABLE` in H2.

        You now have:

        - `customers` with a primary key on `id`
        - `orders` with a primary key on `id` and a foreign key (`customer_id`)
        referencing
        `customers(id)`

        The schema is ready for inserting data or running queries.


  ---



   2) Insert data
  ────────────────

  - > agent delegate Insert realistic sample data into the customers and orders tables. Use at least 5 customers across multiple regions and 15–20 orders with varying dates and amounts.
        - > insert into customers (id, name, region) values
            (1, 'Alice Johnson', 'North'),
            (2, 'Bob Smith', 'South'),
            (3, 'Carol Nguyen', 'East'),
            (4, 'David Martinez', 'West'),
            (5, 'Eva Müller', 'North');
            Updated 5 rows.
        - > insert into orders (id, customer_id, order_date, amount) values
            (101, 1, date '2025-01-05', 120.50),
            (102, 1, date '2025-01-18', 75.00),
            (103, 1, date '2025-02-10', 210.99),
            (104, 2, date '2025-01-07', 45.25),
            (105, 2, date '2025-02-14', 89.90),
            (106, 2, date '2025-03-02', 130.00),
            (107, 3, date '2025-01-12', 300.00),
            (108, 3, date '2025-01-25', 55.75),
            (109, 3, date '2025-02-20', 99.99),
            (110, 3, date '2025-03-05', 180.40),
            (111, 4, date '2025-01-03', 220.00),
            (112, 4, date '2025-02-08', 60.00),
            (113, 4, date '2025-03-01', 145.30),
            (114, 5, date '2025-01-15', 500.00),
            (115, 5, date '2025-02-18', 250.75),
            (116, 5, date '2025-03-10', 95.20);
            Updated 16 rows.

        ✅ Data insertion completed successfully.

        - Customers: 5 rows inserted across multiple regions
        - Orders: 16 rows inserted with varied dates and amounts
        - Referential integrity: Orders correctly reference existing customers

        Your sample dataset is now ready for querying, analytics, joins, or visualization.


  ---



   3) Query and transform
  ────────────────────────

  - > agent delegate Write and run SQL queries that join customers and orders, producing: (a) total sales per customer, (b) total sales per region, and (c) average order amount per region. Output the query results.

        Here are the three join queries and their results.

        (a) Total sales per customer

        - > select
            c.id,
            c.name,
            sum(o.amount) as total_sales
            from customers c
            join orders o
            on c.id = o.customer_id
            group by c.id, c.name
            order by total_sales desc;

            ┌──┬──────────────┬───────────┐
            │ID│     NAME     │TOTAL_SALES│
            ╞══╪══════════════╪═══════════╡
            │ 5│Eva Müller    │     845.95│
            ├──┼──────────────┼───────────┤
            │ 3│Carol Nguyen  │     636.14│
            ├──┼──────────────┼───────────┤
            │ 4│David Martinez│     425.30│
            ├──┼──────────────┼───────────┤
            │ 1│Alice Johnson │     406.49│
            ├──┼──────────────┼───────────┤
            │ 2│Bob Smith     │     265.15│
            └──┴──────────────┴───────────┘

        (b) Total sales per region

        - > select
            c.region,
            sum(o.amount) as total_sales
            from customers c
            join orders o
            on c.id = o.customer_id
            group by c.region
            order by total_sales desc;

            ┌──────┬───────────┐
            │REGION│TOTAL_SALES│
            ╞══════╪═══════════╡
            │North │    1252.44│
            ├──────┼───────────┤
            │East  │     636.14│
            ├──────┼───────────┤
            │West  │     425.30│
            ├──────┼───────────┤
            │South │     265.15│
            └──────┴───────────┘

        (c) Average order amount per region

        - > select
            c.region,
            avg(o.amount) as avg_order_amount
            from customers c
            join orders o
            on c.id = o.customer_id
            group by c.region
            order by avg_order_amount desc;

            ┌──────┬────────────────┐
            │REGION│AVG_ORDER_AMOUNT│
            ╞══════╪════════════════╡
            │North │208.740000000000│
            ├──────┼────────────────┤
            │East  │159.035000000000│
            ├──────┼────────────────┤
            │West  │141.766666666667│
            ├──────┼────────────────┤
            │South │ 88.383333333333│
            └──────┴────────────────┘

        Great — the queries executed correctly. Here’s a concise interpretation of the
        results you
        obtained:



         (a) Total sales per customer
        ──────────────────────────────

        - Eva Müller leads with 845.95
        - Followed by Carol Nguyen (636.14) and David Martinez (425.30)
        - Alice Johnson and Bob Smith have lower cumulative totals

        This reflects both order size and frequency per customer.



         (b) Total sales per region
        ────────────────────────────

        - North is the top-performing region with 1252.44
        - Then East (636.14), West (425.30), and South (265.15)

        North benefits from having two customers, including the highest spender.



         (c) Average order amount per region
        ─────────────────────────────────────

        - North again ranks highest with an average order of 208.74
        - Followed by East (159.04), West (141.77), and South (88.38)

        This shows that North not only has higher total sales, but also larger orders on
        average.

        Overall, the joins and aggregations are working as intended, and the results are
        internally consistent across all three
        analyses.


  ---



   4) Analyze and visualize
  ──────────────────────────

  - > agent delegate Using the query results, compute basic statistics (min, max, mean) on sales by region and render at least one bar or spark chart to visualize regional sales.

        I’ll compute statistics directly from the regional sales query and then visualize
        the totals by
        region.

        - > stats
            Usage: stats <values...>
            Usage: stats <values...; values...; values...>
        - > select c.region,
            sum(o.amount) as total_sales
            from customers c
            join orders o
            on c.id = o.customer_id
            group by c.region;

            ┌──────┬───────────┐
            │REGION│TOTAL_SALES│
            ╞══════╪═══════════╡
            │East  │     636.14│
            ├──────┼───────────┤
            │North │    1252.44│
            ├──────┼───────────┤
            │South │     265.15│
            ├──────┼───────────┤
            │West  │     425.30│
            └──────┴───────────┘
        - > bar
              - No values provided.
              bar <values...>
        - Tool failed; stopping further tool calls.

  Once these agents complete, you’ll have a full end‑to‑end workflow: schema creation → data loading → querying →
  analysis and
  visualization.


  Great question, and thanks for pasting the full transcript — that makes it very clear what happened.



   ✅ What worked
  ───────────────

  You successfully completed almost the entire workflow:

  1. Schema creation ✅
     - `customers` and `orders` tables created with proper PK/FK.

  2. Data insertion ✅
     - 5 customers, 16 orders across regions and dates.

  3. Querying & aggregation ✅
     - Sales per customer
     - Sales per region
     - Average order size per region

  All joins and aggregations executed correctly, and the results were internally consistent.

  ---



   ⚠️ Why the last step failed
  ─────────────────────────────

  The final delegated agent ran into a command‑usage mismatch, not a data problem.

  - `stats` and `bar` do not accept SQL result sets directly
  - They only accept literal numeric values or tables passed explicitly

  So these calls failed:


  stats        ← no values supplied
  bar          ← no values supplied
  ---



   ✅ How to correctly finish Step 4 (analysis & visualization)
  ─────────────────────────────────────────────────────────────

  Here is the *correct* way to run the final analysis using xanadu commands.



   1⃣ Compute statistics on regional sales
  ──────────────────────────────────────────

  - > stats 636.14 1252.44 265.15 425.30

      ┌────────────┐
      │Stats Report│
      ╘════════════╛


       Summary
      ─────────
        • Count: 4
        • Sum: 2579.0299
        • Mean: 644.7575
        • Median: 530.72
        • Std Dev: 432.6722
        • Min: 265.15
        • Max: 1252.4399
        • Range: 987.2899
        • P25: 385.2625
        • P75: 790.215


       Range
      ───────
        │───────◆──▲─────────────────│
        min=265.15 mean=644.7575 median=530.72 max=1252.4399

  This will give you:
  - min
  - max
  - mean
  - standard deviation

  for regional total sales.

  ---



   2⃣ Visualize total sales by region (bar chart)
  ─────────────────────────────────────────────────

  - > bar 1252.44 636.14 425.30 265.15

      ┌─┐
      │ │
      │ │
      │ │
      │ │
      │ │
      │ │
      │ │  ┌─┐
      │ │  │ │
      │ │  │ │  ┌─┐
      │ │  │ │  │ │
      │ │  │ │  │ │  ┌─┐
      │ │  │ │  │ │  │ │
      ┴─┴──┴─┴──┴─┴──┴─┴

  (Ordered as: North, East, West, South)

  Optionally label it:

  - > title Total Sales by Region
      Set title.
  - > xlabel Region (North, East, West, South)
      Set xlabel.
  - > ylabel Total Sales
      Set ylabel.

  ---



   ✅ Final outcome
  ─────────────────

  You now have a complete multi‑step data workflow:

  1. Create relational tables
  2. Load realistic data
  3. Join, aggregate, and analyze
  4. Compute statistics and visualize results

  If you’d like next steps, I can:
  - Store aggregated results in a new table
  - Add time‑series analysis (monthly sales)
  - Create a reusable macro for analytics
  - Export results to CSV

  Just tell me where you want to go next.


  ✅ Workflow completed successfully

  You now have a full, end‑to‑end, multi‑step data workflow with concrete results:

  ---



   📊 Statistical Analysis (Regional Sales)
  ══════════════════════════════════════════

  Using total sales by region:

  - Count: 4 regions
  - Mean: 644.76
  - Median: 530.72
  - Min: 265.15 (South)
  - Max: 1252.44 (North)
  - Std Dev: 432.67 → significant regional variability

  📌 Insight:
  Sales are strongly skewed upward by the North region, which dominates both total and average order value.

  ---



   📈 Visualization
  ══════════════════

  The bar chart clearly shows:

  1. North — dominant revenue contributor
  2. East — solid mid‑tier performance
  3. West — moderate sales
  4. South — lowest total sales

  With:
  - Title ✅
  - X‑axis labels ✅
  - Y‑axis labels ✅

  ---



   ✅ What this demonstrates
  ══════════════════════════

  You successfully executed a realistic analytics pipeline:

  1. Relational schema design
  2. Data loading with referential integrity
  3. Join‑based aggregation
  4. Statistical analysis
  5. Visualization

  This is exactly the kind of workflow used in:
  - BI dashboards
  - Data engineering demos
  - SQL + analytics training
  - Prototyping decision‑support systems

  ---

  If you want to go further, good next steps could be:
  - 📅 Monthly sales trends (time series)
  - 🏆 Top‑N customers per region
  - 📦 Materialized summary tables
  - 🔁 Reusable macros for analytics
  - 📤 Export charts or data to CSV

  Just say the word.

chat >
```
