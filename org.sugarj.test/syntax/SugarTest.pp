%% Pretty printing table (see also SugarTest.generated.pp)
%% Need our own ATerm pretty printer as we have extended ATerms with wildcards
[
   present                -- H hs=0 [KW["e"] _1],
   absent                 -- ,
   real-con               -- H hs=0 [_1 KW["."] _2 _3],
   natural                -- _1,
   positive               -- H hs=0 [KW["+"] _1],
   negative               -- H hs=0 [KW["-"] _1],
   quoted                 -- _1,
   unquoted               -- _1,
   int                    -- _1,
   real                   -- _1,
   fun                    -- _1,
   appl                   -- H hs=0 [_1 KW["("] _2 KW[")"]],
   appl.2:iter-star-sep   -- H hs=0 [_1 KW[","]],
   appl                   -- H hs=0 [_1 KW["("] _2 KW[")"]],
   appl.2:iter-star-sep   -- H hs=0 [_1 KW[","]],
   placeholder            -- H hs=0 [KW["<"] _1 KW[">"]],
   list                   -- H hs=0 [KW["["] _1 KW["]"]],
   list.1:iter-star-sep   -- H hs=0 [_1 KW[","]],
   annotated              -- H hs=0 [_1 _2],
   default                -- H hs=0 [KW["{"] _1 KW["}"]],
   default.1:iter-sep     -- H hs=0 [_1 KW[","]],
   wildcard               -- KW["_"],
   wildcardvar            -- KW["..."]
]
